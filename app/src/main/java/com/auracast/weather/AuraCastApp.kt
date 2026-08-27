package com.auracast.weather

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class AuraCastApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Phase 4 — osmdroid needs a user-agent (or tile providers may block requests) and a
        // writable cache dir; point it at app-private cacheDir so no storage permission is needed.
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }

    // Explicit decoder/fetcher registration (Phase 16 Meteocons + Phase 4 radar tiles).
    // R8/minified release builds can strip Coil's service-loader auto-discovery, so both are
    // registered here rather than relied on implicitly. The missing network fetcher was the
    // actual, confirmed (via on-device logging) root cause of radar tiles never loading —
    // this ImageLoader previously only ever needed to read local asset:// SVGs, so it had no
    // fetcher capable of handling http(s):// URLs at all.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                add(OkHttpNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
}
