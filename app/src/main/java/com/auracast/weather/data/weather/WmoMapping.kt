package com.auracast.weather.data.weather

// WMO weather_code → description + icon key (Meteocons Phase 11/16)
// Covers every code Phase 3 requests.
object WmoMapping {
    fun description(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51 -> "Light drizzle"
        53 -> "Moderate drizzle"
        55 -> "Dense drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Slight rain"
        63 -> "Moderate rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71 -> "Slight snow"
        73 -> "Moderate snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80 -> "Slight showers"
        81 -> "Moderate showers"
        82 -> "Violent showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    fun meteoconsKey(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "clear-day" else "clear-night"
        1 -> if (isDay) "partly-cloudy-day" else "partly-cloudy-night"
        2 -> "partly-cloudy"
        3 -> "overcast"
        45, 48 -> "fog"
        51, 53, 55 -> "drizzle"
        61, 63, 65, 80, 81, 82 -> "rain"
        66, 67, 56, 57 -> "sleet"
        71, 73, 75, 77, 85, 86 -> "snow"
        95, 96, 99 -> "thunderstorm"
        else -> "not-available"
    }
}
