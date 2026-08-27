package com.auracast.weather.ui.components

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.auracast.weather.data.ads.AdManager
import com.auracast.weather.data.billing.BillingManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import javax.inject.Inject

/**
 * Phase 10/16 §5.2 — adaptive banner pinned below all forecast content, own fixed-height
 * container, never interleaved between cards. Renders nothing for ad-free subscribers.
 */
@Composable
fun HomeBannerAd(
    adManager: AdManager,
    billingManager: BillingManager,
) {
    val isAdFree by billingManager.isAdFree.collectAsState()
    if (isAdFree) return

    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            val widthPx = view.width.takeIf { it > 0 } ?: ctx.resources.displayMetrics.widthPixels
            val widthDp = with(density) { widthPx.toDp().value.toInt() }
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp)
            AdView(ctx).apply {
                adUnitId = adManager.bannerAdUnitId
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
