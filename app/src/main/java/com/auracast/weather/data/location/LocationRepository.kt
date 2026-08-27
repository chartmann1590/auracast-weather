package com.auracast.weather.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LatLng(val latitude: Double, val longitude: Double)

@kotlinx.serialization.Serializable
data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val countryCode: String? = null,
    val radarStationId: String? = null,
    val source: Source = Source.GPS,
    val id: String = "$latitude,$longitude",
) {
    @kotlinx.serialization.Serializable
    enum class Source { GPS, SEARCH, SAVED }
}

@Singleton
class GeocodingApi @Inject constructor() {
    // Phase 2: Open-Meteo Geocoding — free, no key
    // GET https://geocoding-api.open-meteo.com/v1/search?name={query}&count=10&language=en&format=json
    // Implemented in Phase 2 via Retrofit; stubbed here for foundation build
    suspend fun search(query: String): List<ResolvedLocation> = emptyList()
}

@Singleton
class DeviceLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownOrCurrent(): LatLng? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) cont.resume(LatLng(loc.latitude, loc.longitude))
                else requestFreshLocation(cont)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(cont: kotlinx.coroutines.CancellableContinuation<LatLng?>) {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setDurationMillis(10_000)
            .build()
        client.getCurrentLocation(request, null)
            .addOnSuccessListener { loc -> cont.resume(loc?.let { LatLng(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
    }
}

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceSource: DeviceLocationSource,
    private val geocodingApi: GeocodingApi,
    private val dataStore: LocationDataStore,
) {
    private val _resolvedLocation = MutableStateFlow<ResolvedLocation?>(null)
    val resolvedLocation: StateFlow<ResolvedLocation?> = _resolvedLocation.asStateFlow()

    private var initialBootstrapped = false

    suspend fun ensureInitialLocation() {
        if (initialBootstrapped) return
        initialBootstrapped = true
        // 1) last saved location from DataStore
        val saved = try {
            dataStore.lastLocationFlow.first()
        } catch (_: Exception) { null }
        if (saved != null) {
            _resolvedLocation.value = saved
            return
        }
        // 2) try GPS (requires permission; caller should have requested it)
        val gps = try { resolveFromGps() } catch (_: Exception) { null }
        if (gps != null) {
            dataStore.saveLastLocation(gps)
            return
        }
        // 3) fallback to default (so weather loads even without permission) — user can still search
        val fallback = ResolvedLocation(
            latitude = 30.2672, longitude = -97.7431, label = "Austin, TX", countryCode = "US", source = ResolvedLocation.Source.SAVED
        )
        _resolvedLocation.value = fallback
    }

    // Reverse geocode via Android Geocoder (no key) — Phase 2 task 4
    suspend fun reverseGeocode(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // async overload on API 33+ — wrap for foundation stub
                var label = "$lat, $lon"
                // synchronous fallback for stub
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lon, 1)
                list?.firstOrNull()?.let { addr ->
                    label = listOfNotNull(addr.locality, addr.adminArea, addr.countryName).joinToString(", ")
                }
                label
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lon, 1)
                list?.firstOrNull()?.let { addr ->
                    listOfNotNull(addr.locality, addr.adminArea, addr.countryName).joinToString(", ")
                } ?: "$lat, $lon"
            }
        } catch (_: Exception) { "$lat, $lon" }
    }

    suspend fun resolveFromGps(): ResolvedLocation? {
        val latLng = deviceSource.getLastKnownOrCurrent() ?: return null
        val label = reverseGeocode(latLng.latitude, latLng.longitude)
        return ResolvedLocation(latitude = latLng.latitude, longitude = latLng.longitude, label = label, source = ResolvedLocation.Source.GPS)
            .also { _resolvedLocation.value = it }
    }

    suspend fun search(query: String): List<ResolvedLocation> = geocodingApi.search(query)

    fun selectLocation(location: ResolvedLocation) {
        _resolvedLocation.value = location
    }

    suspend fun selectAndPersist(location: ResolvedLocation) {
        _resolvedLocation.value = location
        try { dataStore.saveLastLocation(location) } catch (_: Exception) {}
    }
}
