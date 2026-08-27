package com.auracast.weather.data.monitoring

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace as FirebaseTrace
import javax.inject.Inject
import javax.inject.Singleton

// Phase 9 — Firebase Crashlytics + Performance Monitoring, wired to the real
// auracast-weather Firebase project (app/google-services.json).
@Singleton
class Monitoring @Inject constructor() {
    private val crashlytics get() = FirebaseCrashlytics.getInstance()
    private val performance get() = FirebasePerformance.getInstance()

    fun recordException(e: Throwable) = crashlytics.recordException(e)
    fun setCustomKey(key: String, value: String) = crashlytics.setCustomKey(key, value)
    fun setUserId(id: String) = crashlytics.setUserId(id)

    /** Phase 9 custom traces — e.g. "gemma4_report_generation", "radar_tile_load". */
    fun trace(name: String, block: Trace.() -> Unit) {
        val t = performance.newTrace(name)
        t.start()
        Trace(t).apply(block)
        t.stop()
    }

    class Trace(private val delegate: FirebaseTrace) {
        fun putMetric(metric: String, value: Long) = delegate.putMetric(metric, value)
        fun putAttribute(attr: String, value: String) = delegate.putAttribute(attr, value)
    }
}
