package com.auracast.weather.data.weather

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// NWS api.weather.gov — free for any use, requires User-Agent (Phase 3 + 8)
interface NwsApi {
    @GET("points/{lat},{lon}")
    suspend fun getPoint(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Header("User-Agent") userAgent: String = "AuraCastWeather (contact@auracast.app)"
    ): NwsPointResponse

    @GET("alerts/active")
    suspend fun activeAlerts(
        @Query("point") point: String, // "lat,lon"
        @Header("User-Agent") userAgent: String = "AuraCastWeather (contact@auracast.app)"
    ): NwsAlertsResponse
}

@Serializable data class NwsPointResponse(
    val properties: NwsPointProperties? = null
)
@Serializable data class NwsPointProperties(
    val forecast: String? = null,
    val forecastHourly: String? = null,
    val forecastGridData: String? = null,
    val radarStation: String? = null,
)
@Serializable data class NwsAlertsResponse(val features: List<NwsAlertFeature> = emptyList())
@Serializable data class NwsAlertFeature(val id: String, val properties: NwsAlertProperties? = null)
@Serializable data class NwsAlertProperties(val headline: String? = null, val severity: String? = null)
