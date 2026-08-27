# Phase 2 — Location Services (GPS + Manual Search)

**Goal:** resolve a lat/lon + display name from either device GPS or free-text user input, with zero API keys.

## Tasks

1. **Runtime permission flow**
   - Request `ACCESS_COARSE_LOCATION` (sufficient for weather granularity; avoid `FINE` to reduce Play Store data-safety friction unless you want it) using `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`.
   - Show rationale UI before requesting; handle "don't ask again" by deep-linking to app settings.
   - Always offer manual search as a first-class alternative, never a fallback-only path — some users will never grant location.

2. **GPS resolution**

```kotlin
class DeviceLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // caller checks permission first
    suspend fun getLastKnownOrCurrent(): LatLng? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) cont.resume(LatLng(loc.latitude, loc.longitude))
                else requestFreshLocation(cont)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(cont: CancellableContinuation<LatLng?>) {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setDurationMillis(10_000)
            .build()
        client.getCurrentLocation(request, null)
            .addOnSuccessListener { loc -> cont.resume(loc?.let { LatLng(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
    }
}
```

3. **Manual location search — no key needed.** Use **Open-Meteo's free Geocoding API** (`https://geocoding-api.open-meteo.com/v1/search?name={query}&count=10&language=en&format=json`) to turn free-text ("Austin, TX" / zip codes / city names worldwide) into lat/lon + display name + country/admin area, with debounced search-as-you-type.

```kotlin
interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") query: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse
}
```

   - Debounce input with `snapshotFlow`/`.debounce(300)` before firing requests.
   - Cache the 5 most-recent searches locally (Room) for offline re-selection and a "Recent locations" chip row.

4. **Reverse geocoding for GPS results** (turn lat/lon into a human label like "Austin, TX") — Open-Meteo's geocoding API doesn't do reverse lookups; use Android's built-in `Geocoder` class (on-device, no key, backed by the OS's local database on many devices, falls back to Google's geocoding service where available) via `Geocoder.getFromLocation()` (API 33+ async overload; use a coroutine wrapper for compatibility down to minSdk 26 with the legacy blocking call off the main thread).

5. **Location state model** — single source of truth `LocationRepository` exposing a `StateFlow<ResolvedLocation>` (`{ lat, lon, label, source: GPS|SEARCH|SAVED }`), persisted (DataStore) so the app reopens to the last location instantly before any network call resolves.

6. **Multi-location support (stretch, still Phase 2 scope since it's just a list on top of the same repository)** — let users save multiple locations and swipe between them; store as an ordered list in DataStore/Room.

## Acceptance criteria
- Granting location permission and opening the app resolves and displays a real city name within ~3s on a physical device.
- Denying permission still lets the user search "London" and get a valid lat/lon within 1–2 keystrokes of debounce.
- Airplane mode + previously-resolved location still shows the last cached label (no blank screen).

## Sources
- Open-Meteo Geocoding API is part of the same free, no-key API family: https://open-meteo.com/
- FusedLocationProviderClient — standard Play Services Location API (`play-services-location:21.3.0`, current per Google Maven at planning time).
