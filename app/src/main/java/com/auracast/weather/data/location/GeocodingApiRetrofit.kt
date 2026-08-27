package com.auracast.weather.data.location

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

// Open-Meteo Geocoding API — free, no key (Phase 2)
// GET https://geocoding-api.open-meteo.com/v1/search?name={query}&count=10&language=en&format=json
interface GeocodingApiRetrofit {
    @GET("v1/search")
    suspend fun search(
        @Query("name") query: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse
}

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null,
    @SerialName("generationtime_ms") val generationTimeMs: Double? = null,
)

@Serializable
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("country_code") val countryCode: String? = null,
    val country: String? = null,
    @SerialName("admin1") val admin1: String? = null,
    @SerialName("admin2") val admin2: String? = null,
    val timezone: String? = null,
)

fun GeocodingResult.toResolvedLocation(): ResolvedLocation {
    val label = listOfNotNull(name, admin1, country).joinToString(", ")
    return ResolvedLocation(
        latitude = latitude,
        longitude = longitude,
        label = label.ifEmpty { name },
        countryCode = countryCode,
        source = ResolvedLocation.Source.SEARCH
    )
}
