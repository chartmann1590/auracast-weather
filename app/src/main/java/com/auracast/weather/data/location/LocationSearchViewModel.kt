package com.auracast.weather.data.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val geocodingRetrofit: GeocodingApiRetrofit,
    private val dataStore: LocationDataStore,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<ResolvedLocation>>(emptyList())
    val results: StateFlow<List<ResolvedLocation>> = _results.asStateFlow()

    private val _recent = MutableStateFlow<List<String>>(emptyList())
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    init {
        viewModelScope.launch {
            _query.debounce(300).distinctUntilChanged().collect { q ->
                if (q.length < 2) {
                    _results.value = emptyList()
                    return@collect
                }
                try {
                    val resp = geocodingRetrofit.search(q)
                    _results.value = resp.results?.map { it.toResolvedLocation() } ?: emptyList()
                } catch (_: Exception) {
                    _results.value = emptyList()
                }
            }
        }
        viewModelScope.launch {
            dataStore.recentSearchesFlow.collect { _recent.value = it }
        }
    }

    fun onQueryChange(q: String) { _query.value = q }
    fun onSelect(location: ResolvedLocation) {
        viewModelScope.launch {
            locationRepository.selectLocation(location)
            dataStore.saveLastLocation(location)
            dataStore.addRecentSearch(location.label)
        }
    }
    fun onRequestGps() {
        viewModelScope.launch {
            locationRepository.resolveFromGps()?.let { dataStore.saveLastLocation(it) }
        }
    }
}
