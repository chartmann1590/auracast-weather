package com.auracast.weather.data.ads

import android.app.Activity
import android.content.Context
import com.auracast.weather.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 10 — AdMob banner + interstitial, gated by [com.auracast.weather.data.billing.BillingManager.isAdFree].
 *
 * Ships wired against Google's official public *test* ad unit IDs (BuildConfig defaults —
 * see app/build.gradle.kts) so ads render safely in every debug build without risking
 * invalid-traffic flags on a real account. Swap `ADMOB_APP_ID`/`ADMOB_BANNER_ID`/
 * `ADMOB_INTERSTITIAL_ID` (AndroidManifest.xml manifestPlaceholders + build.gradle.kts
 * buildConfigField, both currently test values) for the real production IDs once they're
 * issued — nothing else in this class needs to change.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var initialized = false
    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialMillis = 0L
    private var interstitialShownThisSession = false

    val bannerAdUnitId: String get() = BuildConfig.ADMOB_BANNER_ID

    fun initialize() {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context)
        loadInterstitial()
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun shouldShowInterstitial(): Boolean {
        if (interstitialShownThisSession) return false
        if (System.currentTimeMillis() - lastInterstitialMillis < 3 * 60 * 1000L) return false
        return interstitialAd != null
    }

    /** Shows the preloaded interstitial (Phase 10 cadence: once/session, natural transition points only). */
    fun maybeShowInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (!shouldShowInterstitial() || ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial() // preload the next one immediately
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial()
                onDismissed()
            }
        }
        interstitialShownThisSession = true
        lastInterstitialMillis = System.currentTimeMillis()
        ad.show(activity)
    }
}
