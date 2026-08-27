package com.auracast.weather.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Phase 16 — Meteocons (MIT, https://github.com/basmilius/meteocons) integration.
 * Real SVGs live in app/src/main/assets/meteocons/, loaded via Coil3's SVG decoder
 * (registered in AuraCastApp's ImageLoader). If an asset fails to decode (e.g. running
 * on a Coil version without the SVG decoder wired up), we fall back to a Material icon
 * so the app never shows a blank/broken image.
 */
private fun meteoconsAssetFor(wmoCode: Int, isDay: Boolean): String = when (wmoCode) {
    0, 1 -> if (isDay) "clear-day" else "clear-night"
    2 -> if (isDay) "partly-cloudy-day" else "partly-cloudy-night"
    3 -> "cloudy"
    45, 48 -> "fog"
    51, 53, 55, 56, 57 -> "drizzle"
    61, 63, 65, 66, 67, 80, 81, 82 -> "rain"
    71, 73, 75, 77, 85, 86 -> "snow"
    95, 96, 99 -> "thunderstorms"
    else -> "overcast"
}

@Composable
fun WeatherIcon(
    wmoCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val context = LocalContext.current
    val assetName = meteoconsAssetFor(wmoCode, isDay)

    val transition = rememberInfiniteTransition(label = "weatherIconFloat")
    val float by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "weatherIconFloatValue",
    )

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/meteocons/$assetName.svg")
            .crossfade(true)
            .build(),
        error = null,
    )

    val floatOffset = if (animated) float else 0f

    androidx.compose.foundation.Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .size(64.dp)
            .graphicsLayer { translationY = floatOffset },
    )
}

/** Small, non-animated inline icon for dense layouts (hourly strip, daily rows). */
@Composable
fun WeatherIconSmall(wmoCode: Int, isDay: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val assetName = meteoconsAssetFor(wmoCode, isDay)
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/meteocons/$assetName.svg")
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier.size(28.dp),
    )
}

/** Last-resort fallback if asset loading is ever unavailable (e.g. Robolectric unit tests). */
@Composable
fun WeatherIconFallback(wmoCode: Int, modifier: Modifier = Modifier) {
    val isRain = wmoCode in 51..67 || wmoCode in 80..82
    Icon(
        imageVector = if (isRain) Icons.Filled.Cloud else Icons.Filled.WbSunny,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.size(48.dp),
    )
}
