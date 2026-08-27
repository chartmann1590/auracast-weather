# Phase 10 — Monetization: AdMob + Play Billing Subscription

**Goal:** banner + interstitial ads for free users, a subscription that removes them, built on the current, non-deprecated Billing Library.

## AdMob (Google Mobile Ads SDK)

1. Create an AdMob account, link it to the Play Console app listing, create ad units: one **Adaptive Banner** and one **Interstitial**.
2. Dependency: `implementation("com.google.android.gms:play-services-ads:23.6.0")` (pin to latest at implementation time).
3. Initialize once in `Application.onCreate()`:

```kotlin
MobileAds.initialize(this)
```

4. **Banner placement**: bottom of the Home screen, using `AdView` wrapped in an `AndroidView` composable, **adaptive size** (`AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize`) so it scales correctly across device widths. Only render the composable at all if the user is not an active subscriber (see gating below) — don't just hide it, avoid the network call entirely for paying users.
5. **Interstitial placement**: shown at natural transition points only — e.g. after generating an AI report (a satisfying completion moment, not a random interruption) and capped to at most once per app session and a minimum 3-minute gap between shows, to avoid tanking retention. Preload the next interstitial immediately after showing one (`InterstitialAd.load` in the `onAdDismissedFullScreenContent` callback) so it's ready before the next natural trigger point.
6. **Test ads during development**: use Google's published test ad unit IDs, never real ad unit IDs in debug builds — swap via build-type-specific `BuildConfig` fields (`debug` → test IDs, `release` → real IDs).
7. **Consent**: implement Google's User Messaging Platform (UMP) SDK for GDPR/CCPA consent collection before requesting ads in applicable regions — required for AdMob policy compliance, not optional.

## Play Billing (subscription: "AuraCast Plus" — ad-free)

- Build on **Billing Library 9.1** (current major version; **all apps must be on v8+ by Aug 31, 2026**, so building on anything older is a non-starter for a fresh project in 2026).
- Dependency: `implementation("com.android.billingclient:billing-ktx:9.1.0")`.
- Create a single auto-renewing subscription product in Play Console (e.g. `plus_monthly`, `plus_yearly`) with a base plan + optional free-trial offer.

```kotlin
class BillingManager @Inject constructor(@ApplicationContext context: Context) {
    private val client = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    suspend fun queryEntitlement(): Boolean {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        val result = client.queryPurchasesAsync(params)
        return result.purchasesList.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
    }
}
```

- **Entitlement state** exposed as a single app-wide `StateFlow<Boolean>` (`isAdFree`) consumed by both the AdMob gating logic (Phase above) and any UI badges ("AuraCast Plus" checkmark in Settings).
- Verify purchases server-side eventually (Play Developer API) if fraud becomes a concern post-launch; **client-side `queryPurchasesAsync` is sufficient for MVP** — don't over-build a backend for launch (YAGNI).
- Handle `BillingChoiceInfo` (new in 9.1, alternative billing) only if you plan to support user-choice billing in eligible regions — otherwise default flow is fine for a v1 US-first launch.
- Restore purchases automatically on every app start (`queryPurchasesAsync` acknowledges + reconciles state) and manually via a "Restore Purchases" button in Settings for the reinstall/new-device case.

## Acceptance criteria
- Free-tier user sees a banner on Home and at most one interstitial per session, gated to natural transition points.
- Purchasing "AuraCast Plus" (test track) immediately removes all ad surfaces app-wide without a restart.
- Uninstalling and reinstalling, then tapping "Restore Purchases," correctly restores ad-free state for a real Google account that purchased.

## Sources
- Play Billing Library release notes & v9 features: https://developer.android.com/google/play/billing/release-notes
- Billing Library v8+ mandatory deadline (Aug 31, 2026 / extension to Nov 1, 2026): https://www.revenuecat.com/blog/engineering/play-billing-v9
- Play Billing subscriptions guide: https://developer.android.com/google/play/billing/subscriptions
