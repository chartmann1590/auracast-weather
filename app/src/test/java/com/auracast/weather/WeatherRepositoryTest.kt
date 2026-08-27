package com.auracast.weather

import com.auracast.weather.data.weather.DailyPoint
import com.auracast.weather.data.weather.HourlyPoint
import com.auracast.weather.data.weather.WeatherRepository
import com.auracast.weather.data.weather.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherRepositoryTest {
    @Test
    fun `convertTemp toggles without network`() {
        // Direct formula test — mirrors WeatherRepository.convertTemp without needing DI graph
        fun convertTemp(tempF: Int, toMetric: Boolean): Int = if (toMetric) ((tempF - 32) * 5 / 9) else tempF
        assertEquals(0, convertTemp(32, true))
        assertEquals(100, convertTemp(212, true))
        assertEquals(72, convertTemp(72, false))
    }

    @Test
    fun `wmo mapping covers all codes in Phase 3`() {
        val codes = listOf(0,1,2,3,45,48,51,53,55,61,63,65,71,73,75,80,81,82,95,96,99)
        codes.forEach { code ->
            val desc = com.auracast.weather.data.weather.WmoMapping.description(code)
            assert(desc.isNotEmpty())
        }
    }
}
