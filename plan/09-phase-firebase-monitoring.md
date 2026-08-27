# Phase 9 — Firebase Crashlytics & Performance Monitoring

**Goal:** production-grade observability from day one of internal testing, not bolted on after launch.

## Crashlytics

- Already wired at the Gradle level in Phase 1 (`com.google.firebase.crashlytics` plugin + `firebase-crashlytics-ktx`).
- Force a test crash before first internal release to confirm the pipeline end-to-end:

```kotlin
Button(onClick = { throw RuntimeException("Test Crash — remove before release") }) {
    Text("Test Crash")
}
```

- Enable **opt-in non-fatal error reporting** for handled exceptions in critical paths (network failures in `WeatherRepository`, LLM engine failures in `AiReportEngine`, TTS init failures):

```kotlin
runCatching { ... }.onFailure { e ->
    FirebaseCrashlytics.getInstance().recordException(e)
}
```

- Add **custom keys** for triage context that matters to this app specifically: `ai_engine_used` (aicore/litert/template), `location_source` (gps/search), `tts_offline` (bool), `active_locale`. Set via `FirebaseCrashlytics.getInstance().setCustomKey(...)` — makes crash clusters filterable by "only crashes that happened during on-device LLM generation," etc.
- Set `FirebaseCrashlytics.getInstance().setUserId(...)` to a locally-generated anonymous UUID (never PII) so repeat-crash patterns per install are traceable without identifying the person.

## Performance Monitoring

- Auto-instrumentation (app start time, screen rendering, HTTP requests via the OkHttp interceptor) comes free with the plugin — add the Performance Monitoring OkHttp interceptor to the shared `OkHttpClient` used by Retrofit (Phase 3):

```kotlin
OkHttpClient.Builder()
    .addInterceptor(FirebasePerfOkHttpInterceptor())
    .build()
```

- **Custom traces for the two things that actually matter to this app's UX**:

```kotlin
val trace = FirebasePerformance.getInstance().newTrace("gemma4_report_generation")
trace.start()
// ... run inference ...
trace.putMetric("tokens_generated", tokenCount.toLong())
trace.putAttribute("engine", "aicore") // or "litert" / "template"
trace.stop()
```

```kotlin
val radarTrace = FirebasePerformance.getInstance().newTrace("radar_tile_load")
```

- These two custom traces answer the real product questions: "is on-device LLM generation fast enough in the wild across real device tiers?" and "is the radar screen usably fast?" — both are launch-blocking risk areas per the earlier phases' acceptance criteria, so measuring them from day one of internal testing is what makes those criteria enforceable at scale, not just on the dev's own phone.

## Dashboards & alerting

- In Firebase console: set a **Crashlytics velocity alert** (spike detection) routed to email/Slack before public launch.
- Set a **Performance Monitoring alert** on `gemma4_report_generation` p90 duration exceeding a threshold (e.g. 20s) — catches a regression (bad prompt change, bad model swap) before it reaches a full rollout.

## Acceptance criteria
- A forced test crash appears in the Firebase console Crashlytics dashboard within a few minutes.
- The two custom traces appear in the Performance dashboard with real data after a day of internal dogfooding.
- Custom keys (`ai_engine_used`, etc.) are visible and filterable on real crash reports.

## Sources
- Firebase Crashlytics: https://firebase.google.com/docs/crashlytics
- Firebase Crashlytics get-started (custom keys, non-fatals, user IDs are part of the standard documented API surface): https://firebase.google.com/docs/crashlytics/get-started
