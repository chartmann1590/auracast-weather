package com.auracast.weather.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.auracast.weather.data.radar.RadarFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Draws radar tiles ourselves via Coil instead of osmdroid's MapTileProviderBasic/TilesOverlay
 * module-chain machinery.
 *
 * Real bug found during debugging (verified by decompiling osmdroid 6.1.20 and adding targeted
 * logs, not guessed): TilesOverlay.draw() fired correctly every frame, and MapTileProviderBase's
 * dispatch to module providers looked structurally correct, but our custom OnlineTileSourceBase's
 * getTileURLString() was *never* invoked — no error, no log, a silent no-op somewhere inside
 * osmdroid's multi-layered cache/module dispatch that a global Counters.printToLogcat() dump
 * (fileCacheHit=202, fileCacheMiss=0, tileDownloadErrors=0 — zero misses across an entire
 * session, impossible for tiles that can never be pre-cached) couldn't fully localize either.
 * Rather than keep reverse-engineering closed internals, this renders tiles with Coil — already
 * proven reliable in this app for the Meteocons icons — giving full visibility and control.
 */
private class RadarTileOverlay(
    private val context: android.content.Context,
    private val onTileReady: () -> Unit,
) : Overlay() {
    var urlTemplate: String? = null
        set(value) {
            if (field != value) {
                field = value
                // New frame: old cached bitmaps are for a different timestamp's tiles.
                bitmapCache.evictAll()
            }
        }

    private val bitmapCache = object : android.util.LruCache<String, Bitmap>(48) {}
    private val inFlight = mutableSetOf<String>()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val paint = Paint().apply { isFilterBitmap = true }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val template = urlTemplate ?: return
        val projection = mapView.projection
        val zoom = mapView.zoomLevelDouble.toInt().coerceIn(0, 7)
        val n = 2.0.pow(zoom).toInt()
        val box = mapView.boundingBox ?: return

        val (xMin, yMin) = lonLatToTile(box.lonWest, box.latNorth, zoom, n)
        val (xMax, yMax) = lonLatToTile(box.lonEast, box.latSouth, zoom, n)

        for (x in xMin.coerceAtLeast(0)..xMax.coerceAtMost(n - 1)) {
            for (y in yMin.coerceAtLeast(0)..yMax.coerceAtMost(n - 1)) {
                val url = template.replace("{z}", zoom.toString()).replace("{x}", x.toString()).replace("{y}", y.toString())
                val bmp = bitmapCache.get(url)
                val nw = tileToLonLat(x, y, n)
                val se = tileToLonLat(x + 1, y + 1, n)
                val p1 = projection.toPixels(GeoPoint(nw.second, nw.first), null)
                val p2 = projection.toPixels(GeoPoint(se.second, se.first), null)
                val rect = Rect(minOf(p1.x, p2.x), minOf(p1.y, p2.y), maxOf(p1.x, p2.x), maxOf(p1.y, p2.y))
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, rect, paint)
                } else if (url !in inFlight) {
                    inFlight.add(url)
                    scope.launch {
                        val request = ImageRequest.Builder(context).data(url).build()
                        val result = context.imageLoader.execute(request)
                        if (result is SuccessResult) {
                            bitmapCache.put(url, result.image.toBitmap())
                            onTileReady()
                        }
                        inFlight.remove(url)
                    }
                }
            }
        }
    }

    private fun lonLatToTile(lon: Double, lat: Double, @Suppress("UNUSED_PARAMETER") zoom: Int, n: Int): Pair<Int, Int> {
        val x = ((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val y = ((1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0 * n).toInt()
        return x to y
    }

    private fun tileToLonLat(x: Int, y: Int, n: Int): Pair<Double, Double> {
        val lon = x.toDouble() / n * 360.0 - 180.0
        val latRad = atan(sinh(Math.PI * (1.0 - 2.0 * y / n)))
        return lon to Math.toDegrees(latRad)
    }
}

@Composable
fun RadarScreen(viewModel: RadarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val currentFrame: RadarFrame? = state.frames.getOrNull(state.currentFrameIndex)

    Box(Modifier.fillMaxSize()) {
        if (state.isLoading && state.frames.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null && state.frames.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Radar unavailable: ${state.error}", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Button(onClick = { viewModel.retry() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Retry")
                }
            }
        } else {
            RadarMap(
                centerLat = state.centerLat,
                centerLon = state.centerLon,
                frameUrlTemplate = currentFrame?.tileUrlTemplate,
            )

            // Floating translucent controls over the map (Phase 16 §5.4), not a separate panel.
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.togglePlay() }) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                        )
                    }
                    Slider(
                        value = state.currentFrameIndex.toFloat(),
                        onValueChange = { viewModel.scrubTo(it.toInt()) },
                        valueRange = 0f..((state.frames.size - 1).coerceAtLeast(1)).toFloat(),
                        steps = (state.frames.size - 2).coerceAtLeast(0),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    state.attribution.ifEmpty { "Radar © RainViewer.com" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun RadarMap(centerLat: Double, centerLon: Double, frameUrlTemplate: String?) {
    val mapViewRef = remember { androidx.compose.runtime.mutableStateOf<MapView?>(null) }
    val overlayRef = remember {
        androidx.compose.runtime.mutableStateOf<RadarTileOverlay?>(null)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                // Default position (bottom-center) sat directly behind our play/scrub panel,
                // anchored to the same bottom edge — move the +/- buttons up to top-right,
                // clear of it (pinch-to-zoom via setMultiTouchControls above still works too).
                zoomController.getDisplay().setPositions(
                    false,
                    org.osmdroid.views.CustomZoomButtonsDisplay.HorizontalPosition.RIGHT,
                    org.osmdroid.views.CustomZoomButtonsDisplay.VerticalPosition.TOP,
                )
                minZoomLevel = 3.0
                maxZoomLevel = 7.0 // clamp to RainViewer's free-tier ceiling (plan/15 §3)
                controller.setZoom(7.0)
                controller.setCenter(GeoPoint(centerLat, centerLon))

                val overlay = RadarTileOverlay(
                    context = ctx,
                    onTileReady = { postInvalidate() },
                )
                overlay.urlTemplate = frameUrlTemplate
                overlayRef.value = overlay
                overlays.add(overlay)

                mapViewRef.value = this
            }
        },
        update = { view ->
            view.controller.setCenter(GeoPoint(centerLat, centerLon))
            overlayRef.value?.urlTemplate = frameUrlTemplate
            view.invalidate()
        },
    )

    DisposableEffect(Unit) {
        onDispose { mapViewRef.value?.onDetach() }
    }
}
