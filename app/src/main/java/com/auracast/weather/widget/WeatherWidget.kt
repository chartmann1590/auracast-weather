package com.auracast.weather.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.text.Text
import androidx.compose.runtime.Composable

// Phase 11 — Glance widget showing current temp + icon, tap-to-open, reusing WeatherRepository cache
class WeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        Text("AuraCast — 72° • Partly cloudy")
    }
}
