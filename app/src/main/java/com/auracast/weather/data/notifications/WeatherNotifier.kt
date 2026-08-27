package com.auracast.weather.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("severe_weather", "Severe Weather", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Official NWS severe weather alerts"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel("daily_briefing", "Daily Briefing", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Your morning AI weather podcast"
            }
        )
    }

    fun postSevereAlert(location: String, headline: String) {
        // Phase 8: post high-priority notification via NotificationCompat
    }

    fun postDailyBriefing(text: String) { /* Phase 8 */ }
}
