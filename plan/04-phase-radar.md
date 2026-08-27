# Phase 4 — Animated Precipitation Radar

**Goal:** a pinch-zoom, animated radar map, no API key, sourced from NWS (US) or RainViewer (everywhere else).

> ⚠️ **Licensing gate before launch.** RainViewer's own docs state its free API is for **"personal and educational use only"** — stricter wording than "non-commercial," and this app (ads + subscriptions) doesn't qualify as either. Ship international radar behind this resolved: either a RainViewer commercial license is in place, or the radar tab is scoped to US-only (NWS) locations at launch with international radar added later. Full detail in `15-rate-limits-and-quotas.md` §3. Don't build past this gate assuming it'll sort itself out.

## Data sources

### RainViewer (global, no key)
1. `GET https://api.rainviewer.com/public/weather-maps.json` → returns a timeline of past-radar frame timestamps (past 2h + a nowcast) and a `host` + tile URL template.
2. Tile URL pattern: `{host}/{path}/{size}/{z}/{x}/{y}/{color}/{options}.png` — e.g. `.../256/{z}/{x}/{y}/2/1_1.png`.
3. **Free-tier constraints to design around**: max zoom level 7 (256/512px tiles), "Universal Blue" color scheme only, past frames only (no forecast/nowcast layer, no satellite layer) on the free public endpoint. Build the animation loop from only the frames the JSON actually returns — don't assume a fixed count.
4. Attribution required: visible "Radar © RainViewer.com" credit on the map screen.

### NWS radar (US, free for any use)
- Each NWS office/point response includes a `radarStation` (nearest WSR-88D site ID). Use NOAA's public radar tile/image services (via `api.weather.gov` station metadata and NOAA's radar imagery endpoints) for higher-resolution official CONUS/regional radar when the resolved location is in the US.
- Prefer NWS here because RainViewer's free tier is capped at low zoom — NWS gives US users a materially better radar experience, which matters since that's the primary launch market.

## Map rendering approach

- Use `MapLibre` (open-source, no Google Maps API key/billing required) or `osmdroid` as the base map, with the radar source layered as a raster tile overlay. **Do not use Google Maps SDK** for this screen unless you're fine adding a billing-gated API key — contradicts the "no API key" requirement.
- Tile overlay: implement a custom `TileProvider` that requests the current animation-frame's tile URL for each `(z,x,y)`, backed by an OkHttp disk cache (radar tiles refresh every ~5–10 min, so a short-TTL disk cache avoids re-downloading during scrub/zoom).
- **Animation controls**: play/pause, scrub bar across available frames, frame interval ~500ms, loop with a brief pause on the latest frame.
- **Location pin + radius circle** overlay so users have a fixed reference point while panning.

## Repository design

```kotlin
class RadarRepository @Inject constructor(
    private val rainViewer: RainViewerApi,
    private val nws: NwsApi,
) {
    suspend fun getTimeline(location: ResolvedLocation): RadarTimeline =
        if (location.countryCode == "US") nws.radarTimeline(location.radarStationId)
        else rainViewer.timeline()
}
```

## Acceptance criteria
- Radar screen loads and animates within 3s on Wi-Fi for a US location (NWS) and an international location (RainViewer).
- Pinch-zoom and pan work smoothly (60fps target) with tiles loading progressively, no crash at RainViewer's zoom-7 ceiling (clamp max zoom client-side for non-US locations).
- Attribution text is visible and unobscured by UI chrome, satisfying RainViewer's usage terms.

## Sources
- RainViewer API docs & free-tier limits: https://www.rainviewer.com/api.html
- NWS API (radar station metadata via point lookup): https://weather-gov.github.io/api/
- Full rate-limit/licensing detail for both: `15-rate-limits-and-quotas.md`
