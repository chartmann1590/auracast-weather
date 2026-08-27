package com.auracast.weather.data.radar

import com.auracast.weather.data.location.ResolvedLocation
import javax.inject.Inject
import javax.inject.Singleton

data class RadarTimeline(val frames: List<RadarFrame>, val attribution: String)
data class RadarFrame(val timestamp: Long, val tileUrlTemplate: String) // {z}/{x}/{y} placeholders

/**
 * Phase 4 — RainViewer public tile API (global, no key). Verified live:
 * https://www.rainviewer.com/api.html
 *
 * NWS radar (US-preferred per the plan, better resolution, no free-tier zoom cap) is not
 * wired in this pass — RainViewer covers every location including the US for now. Swapping
 * in NWS raster tiles for US-resolved locations (using NwsPointProperties.radarStation) is
 * a follow-up, tracked in plan/04-phase-radar.md.
 */
@Singleton
class RadarRepository @Inject constructor(
    private val api: RainViewerApiRetrofit,
) {
    suspend fun getTimeline(location: ResolvedLocation): RadarTimeline = rainViewerTimeline()

    suspend fun rainViewerTimeline(): RadarTimeline {
        val resp = api.getMaps()
        val host = resp.host ?: "https://tilecache.rainviewer.com"
        val pastFrames = resp.radar?.past.orEmpty()
        val frames = pastFrames.map { f ->
            // Free-tier constraints (plan/15 §3): max zoom 7, Universal Blue color scheme (2),
            // 256px tiles, past-only. {z}/{x}/{y} filled in by the tile source at render time.
            RadarFrame(
                timestamp = f.time,
                tileUrlTemplate = "$host${f.path}/256/{z}/{x}/{y}/2/1_1.png",
            )
        }
        return RadarTimeline(frames = frames, attribution = "Radar © RainViewer.com")
    }
}
