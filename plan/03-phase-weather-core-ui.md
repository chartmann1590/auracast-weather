# Phase 3 — Core Weather Data & UI (Current / Hourly / 5-Day)

**Goal:** real forecast data on screen, cached for offline, unit-aware, from Open-Meteo (global) with NWS as the preferred source for US-resolved locations.

## Data sources

### Open-Meteo Forecast API (global, no key)
`GET https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m,uv_index&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch&timezone=auto&forecast_days=7`

- Returns current conditions, hourly (up to 16 days available, we'll request/display 48h), and daily (7-day, we'll display 5) in one call — cheap on rate limits.
- `weather_code` is WMO code; map to icon + description via a static lookup table (build once, see Phase 11 for icon assets).
- **Licensing reminder (see main plan §5):** this is the non-commercial free tier. Must move to the Standard commercial plan (or drop to NWS-only for US) before the monetized build ships.

### NWS API (US-resolved locations, free for any use)
Two-step flow:
1. `GET https://api.weather.gov/points/{lat},{lon}` → returns `forecast`, `forecastHourly`, and `forecastGridData` URLs plus the nearest radar station ID.
2. `GET` those URLs for daily / hourly forecast text + values (GeoJSON).
- Set a descriptive `User-Agent` header (NWS API requires one identifying the app + contact, e.g. `"AuraCastWeather (contact@yourdomain.com)"`) — required by their usage policy.
- Use NWS as primary for US lat/lon (better local accuracy + official severe-weather alerts, feeds Phase 8 notifications); fall back to Open-Meteo if NWS is unreachable or the point falls outside its grid (rare, e.g. some territories).

## Repository design

```kotlin
class WeatherRepository @Inject constructor(
    private val openMeteo: OpenMeteoApi,
    private val nws: NwsApi,
    private val dao: WeatherCacheDao,
) {
    suspend fun getForecast(location: ResolvedLocation): Result<WeatherSnapshot> {
        val source = if (location.countryCode == "US") ::fetchNws else ::fetchOpenMeteo
        return runCatching { source(location) }
            .recoverCatching { fetchOpenMeteo(location) }  // universal fallback
            .onSuccess { dao.upsert(it.toEntity(location.id)) }
            .onFailure { /* fall through to cache below */ }
            .let { result ->
                result.getOrNull()?.let { Result.success(it) }
                    ?: dao.get(location.id)?.let { Result.success(it.toDomain()) }
                    ?: Result.failure(result.exceptionOrNull() ?: IOException("no data"))
            }
    }
}
```

- Cache-then-network pattern: emit cached `WeatherSnapshot` immediately (Flow), then emit the fresh network result when it lands. Cache TTL ~15 minutes for current, ~1 hour for hourly/daily.
- Retrofit + OkHttp with a 10s connect / 15s read timeout and a single retry-with-backoff interceptor (weather APIs are usually fast but public free tiers can hiccup under load).

## UI (Jetpack Compose, Material 3)

- **Home screen**: large current-temp hero card with animated weather icon (Lottie or Compose `Canvas` — see Phase 11) over a dynamic gradient background (color keyed to weather_code + time-of-day: clear-day blue/gold, night indigo, storm slate).
- **Hourly strip**: horizontally scrollable `LazyRow` of the next 48h, each item temp + icon + precip probability bar.
- **5-day list**: `LazyColumn`, each row = day name, icon, condition text, high/low with a min–max range bar visualizing the week's overall range.
- **Pull-to-refresh** via `PullToRefreshBox` (Material3).
- **Unit toggle** (°F/°C, mph/km/h, in/mm) persisted in DataStore, applied at the repository query level (Open-Meteo takes unit params directly; NWS values are converted client-side since NWS returns SI units).

## Acceptance criteria
- Cold launch to first paint of real current-conditions data in under 2s on cached location, under 4s on first-ever GPS resolve, on a mid-range device/emulator.
- Killing network mid-session still shows last-cached data with a "showing cached data from Xm ago" banner.
- Toggling °F/°C updates every visible number without a network round-trip.

## Sources
- Open-Meteo API docs & parameters: https://open-meteo.com/en/docs
- Open-Meteo pricing/non-commercial terms: https://open-meteo.com/en/pricing
- NWS API: https://weather-gov.github.io/api/ (User-Agent requirement documented in their general FAQ)
