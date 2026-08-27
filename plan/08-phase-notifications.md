# Phase 8 — Notifications (Local + Push)

**Goal:** severe weather alerts and a daily AI-report summary notification, without needing a backend server for the MVP.

## Notification types

1. **Severe weather alerts (local, data-driven, highest value)**
   - US locations: poll `api.weather.gov/alerts/active?point={lat},{lon}` — official NWS active-alerts feed, free, no key.
   - International locations: Open-Meteo doesn't provide alerts; ship this feature as US-only at launch and document the gap rather than faking it.
   - Trigger: a `WorkManager` periodic worker (`PeriodicWorkRequest`, minimum 15-minute interval — the OS floor) checks alerts for the user's saved location(s) and posts a high-priority notification (`NotificationCompat.PRIORITY_HIGH`, dedicated "Severe Weather" channel) when a new alert ID appears that wasn't seen last run (dedupe via a stored set of alert IDs in DataStore).

2. **Daily AI weather podcast notification**
   - User-configurable time (Settings → Notifications → "Morning briefing at ___"), scheduled via `WorkManager` `PeriodicWorkRequest` anchored with `setInitialDelay` computed to the next occurrence of the chosen time, chained to re-schedule itself each run (periodic work can't natively pin to wall-clock time, so recompute delay each cycle).
   - Tapping the notification deep-links straight into the Report tab and auto-plays if a "auto-play on open" setting is enabled.

3. **Firebase Cloud Messaging (FCM) — future-proofing, not core MVP**
   - Wire up `FirebaseMessagingService` and topic subscription (e.g. subscribe devices to a `severe-weather-national` topic) so that **if/when** a backend is added later, server-initiated push works with zero client changes. For the MVP with no backend, FCM is initialized but unused beyond receiving the token (useful for future targeted re-engagement campaigns via Firebase console "Send test message" / Cloud Messaging campaigns, which don't require custom backend code).

## Implementation sketch

```kotlin
class SevereWeatherCheckWorker(
    context: Context,
    params: WorkerParameters,
    private val nws: NwsApi,
    private val seenAlerts: SeenAlertsStore,
    private val notifier: WeatherNotifier,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val locations = /* saved locations, US only */
        locations.forEach { loc ->
            val alerts = runCatching { nws.activeAlerts(loc.lat, loc.lon) }.getOrNull() ?: return@forEach
            alerts.features.filterNot { seenAlerts.contains(it.id) }.forEach { alert ->
                notifier.postSevereAlert(loc, alert)
                seenAlerts.markSeen(alert.id)
            }
        }
        return Result.success()
    }
}
```

- Notification channels (Android 8+, required): `severe_weather` (HIGH importance, default sound), `daily_briefing` (DEFAULT importance), created at app startup via `NotificationManager.createNotificationChannel`.
- Respect Android 13+ `POST_NOTIFICATIONS` runtime permission — request it contextually (e.g. right after the user enables the daily briefing toggle), not on cold launch.

## Acceptance criteria
- A simulated NWS alert (mock the API response in a debug build) produces a distinct, high-priority notification within one 15-minute worker cycle.
- Daily briefing notification fires within ±1 minute of the configured time across a device reboot (verify `WorkManager` persistence survives reboot — requires `RECEIVE_BOOT_COMPLETED` handling to re-enqueue if using exact scheduling, though periodic `WorkManager` jobs are reboot-persistent by default).
- Disabling notifications in Settings cancels all pending work (`WorkManager.cancelUniqueWork`) immediately, verified via `adb shell dumpsys jobscheduler`.

## Sources
- NWS active alerts endpoint: https://weather-gov.github.io/api/ (alerts resource)
- Firebase Cloud Messaging Android setup: https://firebase.google.com/docs/crashlytics (Firebase Android product suite overview, FCM is part of the same BoM used in Phase 1/9)
