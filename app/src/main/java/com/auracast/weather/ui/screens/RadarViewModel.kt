package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracast.weather.data.location.LocationRepository
import com.auracast.weather.data.radar.RadarFrame
import com.auracast.weather.data.radar.RadarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadarUiState(
    val isLoading: Boolean = true,
    val frames: List<RadarFrame> = emptyList(),
    val currentFrameIndex: Int = 0,
    val isPlaying: Boolean = true,
    val attribution: String = "",
    val error: String? = null,
    val centerLat: Double = 39.8283,
    val centerLon: Double = -98.5795, // continental-US-ish default until location resolves
)

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val radarRepository: RadarRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loc = locationRepository.resolvedLocation.first { it != null }
            if (loc != null) {
                _uiState.value = _uiState.value.copy(centerLat = loc.latitude, centerLon = loc.longitude)
            }
        }
        loadTimeline()
        startAnimationLoop()
    }

    private fun loadTimeline() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { radarRepository.rainViewerTimeline() }
                .onSuccess { timeline ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        frames = timeline.frames,
                        currentFrameIndex = (timeline.frames.size - 1).coerceAtLeast(0),
                        attribution = timeline.attribution,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Radar unavailable")
                }
        }
    }

    private fun startAnimationLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(500) // Phase 4: ~500ms frame interval
                val s = _uiState.value
                if (s.isPlaying && s.frames.isNotEmpty()) {
                    val atEnd = s.currentFrameIndex >= s.frames.size - 1
                    // brief pause on the latest frame before looping back to the start
                    if (atEnd) delay(1500)
                    val next = if (atEnd) 0 else s.currentFrameIndex + 1
                    _uiState.value = s.copy(currentFrameIndex = next)
                }
            }
        }
    }

    fun togglePlay() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun scrubTo(index: Int) {
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            currentFrameIndex = index.coerceIn(0, (_uiState.value.frames.size - 1).coerceAtLeast(0)),
        )
    }

    fun retry() = loadTimeline()
}
