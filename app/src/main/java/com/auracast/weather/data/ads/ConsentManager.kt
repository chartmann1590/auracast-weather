package com.auracast.weather.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 10 — UMP consent for AdMob (required for GDPR/CCPA).
 * Called from MainActivity before MobileAds.initialize so EEA users see consent before any ad request.
 * In DEBUG, you can enable geography=EEA via [ConsentDebugSettings] to test the dialog without VPN.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    fun requestConsent(activity: Activity, onResult: (Boolean) -> Unit = {}) {
        val paramsBuilder = ConsentRequestParameters.Builder()
        // DEBUG helper: uncomment to force EEA geography for testing (requires test device hash)
        // paramsBuilder.setConsentDebugSettings(
        //     ConsentDebugSettings.Builder(activity)
        //         .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        //         .addTestDeviceHashedId("YOUR_TEST_HASH")
        //         .build()
        // )
        val params = paramsBuilder.build()

        consentInfo.requestConsentInfoUpdate(params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                // error == null means form shown and dismissed or not required; we can now init MobileAds
                onResult(error == null)
            }
        }, { error ->
            // Failed to update — proceed without consent form (still initialize ads, they will be non-personalized)
            onResult(false)
        })

        if (consentInfo.canRequestAds()) {
            // Already have consent or not required — ensure form is not needed again
            // loadAndShowConsentFormIfRequired handles this; nothing extra to do.
        }
    }

    fun canRequestAds(): Boolean = consentInfo.canRequestAds()

    fun resetForTesting() {
        // For QA: clear consent to re-prompt. Call only in debug builds.
        consentInfo.reset()
    }
}
