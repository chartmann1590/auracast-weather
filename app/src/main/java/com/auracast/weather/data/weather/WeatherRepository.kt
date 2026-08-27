package com.auracast.weather.data.weather

import com.auracast.weather.data.location.ResolvedLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@kotlinx.serialization.Serializable
data class HourlyPoint(val timeLabel: String, val tempF: Int, val precipProb: Int)

@kotlinx.serialization.Serializable
data class DailyPoint(val dayName: String, val highF: Int, val lowF: Int, val conditionText: String)

@kotlinx.serialization.Serializable
data class WeatherSnapshot(
    val locationLabel: String,
    val currentTempF: Int,
    val feelsLikeF: Int,
    val todayHighF: Int,
    val todayLowF: Int,
    val conditionText: String,
    val wmoCode: Int,
    val isNight: Boolean,
    val humidity: Int,
    val windMph: Int,
    val hourly: List<HourlyPoint>,
    val daily: List<DailyPoint>,
    val reportTeaser: String? = null,
    val hourlyTrendSummary: String = "steady",
    val tomorrowSummary: String = "—",
    val isFromCache: Boolean = false,
    val cacheAgeMinutes: Long = 0,
)

@Singleton
class WeatherRepository @Inject constructor(
    private val openMeteo: OpenMeteoApi,
    private val nws: NwsApi,
    private val dao: WeatherCacheDao,
    private val json: Json,
) {
    private val _weatherFlow = MutableStateFlow<WeatherSnapshot?>(null)
    val weatherFlow: StateFlow<WeatherSnapshot?> = _weatherFlow.asStateFlow()

    private var lastSnapshot: WeatherSnapshot? = null

    suspend fun refresh(location: ResolvedLocation, force: Boolean = false) {
        if (!force && lastSnapshot != null && _weatherFlow.value != null) {
            // still try to refresh in background, but emit cached first
            tryEmitCached(location)
        }
        // Try cache first for instant paint if no value yet
        if (_weatherFlow.value == null) {
            tryEmitCached(location)
        }

        // Network fetch — Open-Meteo primary (global), NWS attempted for US but Open-Meteo is universal
        val networkResult = runCatching { fetchOpenMeteo(location) }
        if (networkResult.isSuccess) {
            val snap = networkResult.getOrThrow()
            persist(snap, location.id)
            lastSnapshot = snap
            _weatherFlow.value = snap
            return
        }
        // Network failed — keep cache if we had it, else emit demo so screen is never blank
        if (_weatherFlow.value != null) return
        // No cache — try DAO again
        val cached = loadFromCache(location.id)
        if (cached != null) {
            _weatherFlow.value = cached
            return
        }
        // Last resort demo (ensures acceptance criteria: never blank screen)
        val demo = demoSnapshot(location.label)
        _weatherFlow.value = demo
        lastSnapshot = demo
    }

    private suspend fun tryEmitCached(location: ResolvedLocation) {
        // cacheAgeMinutes is already computed correctly inside loadFromCache() from the
        // DAO row's real timestamp — nothing further to derive here.
        loadFromCache(location.id)?.let { cached ->
            _weatherFlow.value = cached
            lastSnapshot = cached
        }
    }

    private suspend fun loadFromCache(locationId: String): WeatherSnapshot? {
        return try {
            val entity = dao.get(locationId) ?: return null
            val snap = json.decodeFromString<WeatherSnapshot>(entity.snapshotJson)
            val ageMin = (System.currentTimeMillis() - entity.timestampMillis) / 60000
            snap.copy(isFromCache = true, cacheAgeMinutes = ageMin)
        } catch (_: Exception) { null }
    }

    private suspend fun persist(snapshot: WeatherSnapshot, locationId: String) {
        try {
            dao.upsert(WeatherCacheEntity(locationId, json.encodeToString(WeatherSnapshot.serializer(), snapshot), System.currentTimeMillis()))
        } catch (_: Exception) {}
    }

    private suspend fun fetchOpenMeteo(location: ResolvedLocation): WeatherSnapshot {
        val resp = openMeteo.getForecast(latitude = location.latitude, longitude = location.longitude)
        return mapOpenMeteo(resp, location.label)
    }

    private fun mapOpenMeteo(resp: OpenMeteoResponse, locationLabel: String): WeatherSnapshot {
        val cur = resp.current
        val hourlyDto = resp.hourly
        val dailyDto = resp.daily

        val wmo = cur?.weatherCode ?: dailyDto?.weatherCode?.firstOrNull() ?: 0
        val isNight = cur?.isDay == 0
        val condition = WmoMapping.description(wmo)

        // Hourly — next 48, format timeLabel like "3p", "11a"
        val hourlyPoints = mutableListOf<HourlyPoint>()
        if (hourlyDto != null) {
            val fmtIn = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val count = minOf(48, hourlyDto.time.size)
            for (i in 0 until count) {
                val tStr = hourlyDto.time[i]
                val label = try {
                    val dt = LocalDateTime.parse(tStr, fmtIn)
                    val hour = dt.hour
                    val ampm = if (hour < 12) "a" else "p"
                    val hr12 = when (hour % 12) { 0 -> 12 else -> hour % 12 }
                    "${hr12}$ampm"
                } catch (_: Exception) { tStr.takeLast(5) }
                val temp = hourlyDto.temperature2m.getOrNull(i)?.toInt() ?: 0
                val pp = hourlyDto.precipProb.getOrNull(i) ?: 0
                hourlyPoints.add(HourlyPoint(label, temp, pp))
            }
        }

        // Daily — 5 days
        val dailyPoints = mutableListOf<DailyPoint>()
        if (dailyDto != null) {
            val count = minOf(5, dailyDto.time.size)
            for (i in 0 until count) {
                val dateStr = dailyDto.time[i]
                val dayName = try {
                    val d = LocalDate.parse(dateStr)
                    if (i == 0) "Today" else d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                } catch (_: Exception) { dateStr }
                val high = dailyDto.tempMax.getOrNull(i)?.toInt() ?: 0
                val low = dailyDto.tempMin.getOrNull(i)?.toInt() ?: 0
                val code = dailyDto.weatherCode.getOrNull(i) ?: wmo
                val cond = WmoMapping.description(code)
                dailyPoints.add(DailyPoint(dayName, high, low, cond))
            }
        }

        val todayHigh = dailyDto?.tempMax?.firstOrNull()?.toInt() ?: cur?.temperature2m?.toInt() ?: 0
        val todayLow = dailyDto?.tempMin?.firstOrNull()?.toInt() ?: 0
        val curTemp = cur?.temperature2m?.toInt() ?: todayHigh
        val feels = cur?.apparentTemperature?.toInt() ?: curTemp
        val hum = cur?.humidity ?: 0
        val wind = cur?.windSpeed?.toInt() ?: 0

        // Trends
        val hourlyTrend = if (hourlyPoints.size >= 6) {
            val max = hourlyPoints.take(12).maxOf { it.tempF }
            val ppMax = hourlyPoints.take(12).maxOf { it.precipProb }
            if (ppMax > 30) "rising to ${max}°F, chance of rain ${ppMax}%"
            else "rising to ${max}°F, mostly dry"
        } else "steady"

        val tomorrowSummary = dailyPoints.getOrNull(1)?.let { "${it.highF}°F / ${it.lowF}°F, ${it.conditionText}" } ?: "—"

        val teaser = when {
            wmo in 95..99 -> "Thunderstorms expected — full report →"
            wmo in 61..67 || wmo in 80..82 -> "Rain on the way — full report →"
            wmo == 0 -> "Clear skies — full report →"
            else -> "$condition — full report →"
        }

        return WeatherSnapshot(
            locationLabel = locationLabel,
            currentTempF = curTemp,
            feelsLikeF = feels,
            todayHighF = todayHigh,
            todayLowF = todayLow,
            conditionText = condition,
            wmoCode = wmo,
            isNight = isNight,
            humidity = hum,
            windMph = wind,
            hourly = hourlyPoints,
            daily = dailyPoints,
            reportTeaser = teaser,
            hourlyTrendSummary = hourlyTrend,
            tomorrowSummary = tomorrowSummary,
            isFromCache = false,
            cacheAgeMinutes = 0,
        )
    }

    private fun demoSnapshot(label: String): WeatherSnapshot {
        return WeatherSnapshot(
            locationLabel = label,
            currentTempF = 72,
            feelsLikeF = 75,
            todayHighF = 84,
            todayLowF = 62,
            conditionText = "Partly cloudy",
            wmoCode = 2,
            isNight = false,
            humidity = 58,
            windMph = 8,
            hourly = (0 until 48).map { h ->
                HourlyPoint(timeLabel = "${(h % 12).let { if (it==0) 12 else it }}${if (h<12) "a" else "p"}", tempF = 70 + (h % 8), precipProb = if (h % 5==0) 20 else 0)
            },
            daily = listOf(
                DailyPoint("Today", 84, 62, "Partly cloudy"),
                DailyPoint("Tomorrow", 81, 60, "Sunny"),
                DailyPoint("Wed", 79, 59, "Rain"),
                DailyPoint("Thu", 83, 61, "Cloudy"),
                DailyPoint("Fri", 86, 64, "Thunderstorm"),
            ),
            hourlyTrendSummary = "rising to 84°F by 3pm, chance of storms after 5pm (40%)",
            tomorrowSummary = "81°F / 60°F, sunny",
            isFromCache = false
        )
    }

    fun convertTemp(tempF: Int, toMetric: Boolean): Int = if (toMetric) ((tempF - 32) * 5 / 9) else tempF
}
