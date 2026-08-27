package com.auracast.weather.data.radar

import kotlinx.serialization.Serializable
import retrofit2.http.GET

// RainViewer public tile API — no key, free tier caps noted in plan/04 and 15
// GET https://api.rainviewer.com/public/weather-maps.json
interface RainViewerApiRetrofit {
    @GET("public/weather-maps.json")
    suspend fun getMaps(): RainViewerMapsResponse
}

@Serializable
data class RainViewerMapsResponse(
    val version: String? = null,
    val generated: Long? = null,
    val host: String? = null,
    val radar: RadarSection? = null,
    val satellite: SatelliteSection? = null,
)

@Serializable data class RadarSection(
    val past: List<RadarFrameDto> = emptyList(),
    val nowcast: List<RadarFrameDto> = emptyList(),
)
@Serializable data class SatelliteSection(val infrared: List<RadarFrameDto> = emptyList())
@Serializable data class RadarFrameDto(val time: Long, val path: String)
