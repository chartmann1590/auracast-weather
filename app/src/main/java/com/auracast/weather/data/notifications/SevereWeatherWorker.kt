package com.auracast.weather.data.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Phase 8 — periodic NWS alert poll (15m floor, US-only)
@HiltWorker
class SevereWeatherCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Fetch api.weather.gov/alerts/active?point={lat},{lon}
        // Dedup via stored alert IDs in DataStore, post via WeatherNotifier
        return Result.success()
    }
}

@HiltWorker
class DailyBriefingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}
