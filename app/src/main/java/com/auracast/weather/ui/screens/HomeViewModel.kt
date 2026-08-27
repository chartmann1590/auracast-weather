package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracast.weather.data.location.LocationRepository
import com.auracast.weather.data.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HourlyUi(val timeLabel: String, val temp: Int, val precipProb: Int)
data class DailyUi(val dayName: String, val high: Int, val low: Int, val condition: String)

data class HomeUiState(
    val isLoading: Boolean = true,
    val currentTemp: Int = 0,
    val highTemp: Int = 0,
    val lowTemp: Int = 0,
    val conditionText: String = "Loading…",
    val locationLabel: String = "Locating…",
    val wmoCode: Int = 0,
    val isNight: Boolean = false,
    val hourly: List<HourlyUi> = emptyList(),
    val daily: List<DailyUi> = emptyList(),
    val reportTeaser: String = "Generate your AI weather report →",
    val showCachedBanner: Boolean = false,
    val cachedAgeMinutes: Long = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Bootstrap location (DataStore -> GPS -> fallback Austin) then weather
        viewModelScope.launch {
            try { locationRepository.ensureInitialLocation() } catch (_: Exception) {}
        }

        viewModelScope.launch {
            // Cache-then-network: emit cached immediately, then fresh
            combine(
                locationRepository.resolvedLocation,
                weatherRepository.weatherFlow
            ) { loc, snapshot ->
                // Map domain to UI; snapshot may be null on first launch
                if (snapshot == null) {
                    HomeUiState(
                        locationLabel = loc?.label ?: "Locating…",
                        conditionText = if (loc == null) "Locating…" else "Loading…",
                        isLoading = true
                    )
                } else {
                    val isNight = snapshot.isNight
                    HomeUiState(
                        isLoading = false,
                        currentTemp = snapshot.currentTempF,
                        highTemp = snapshot.todayHighF,
                        lowTemp = snapshot.todayLowF,
                        conditionText = snapshot.conditionText,
                        locationLabel = loc?.label ?: snapshot.locationLabel,
                        wmoCode = snapshot.wmoCode,
                        isNight = isNight,
                        hourly = snapshot.hourly.take(48).map { h ->
                            HourlyUi(h.timeLabel, h.tempF, h.precipProb)
                        },
                        daily = snapshot.daily.take(5).map { d ->
                            DailyUi(d.dayName, d.highF, d.lowF, d.conditionText)
                        },
                        reportTeaser = snapshot.reportTeaser ?: "Sunny with a late storm — full report →",
                        showCachedBanner = snapshot.isFromCache,
                        cachedAgeMinutes = snapshot.cacheAgeMinutes
                    )
                }
            }.collect { _uiState.value = it }
        }

        viewModelScope.launch {
            // Trigger a refresh when location resolves — debounced to avoid double fetch on init
            locationRepository.resolvedLocation.collect { loc ->
                if (loc != null) {
                    try { weatherRepository.refresh(loc) } catch (_: Exception) {}
                }
            }
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            try {
                val gps = locationRepository.resolveFromGps()
                if (gps != null) weatherRepository.refresh(gps, force = true)
            } catch (_: Exception) {}
        }
    }

    fun onPullToRefresh() {
        viewModelScope.launch {
            locationRepository.resolvedLocation.value?.let { weatherRepository.refresh(it, force = true) }
        }
    }
}
