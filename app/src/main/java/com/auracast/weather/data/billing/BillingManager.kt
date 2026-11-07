package com.auracast.weather.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.hartmannstudios.direct.DirectBilling
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Phase 10 — Play Billing Library 9.1, subscription: "AuraCast Plus" (ad-free).
 *
 * Product IDs (`plus_monthly` / `plus_yearly`) are placeholders until real subscription
 * products are configured in Play Console — the billing flow itself is real and will work
 * unchanged once those products exist.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val PRODUCT_MONTHLY = "plus_monthly"
        const val PRODUCT_YEARLY = "plus_yearly"
        // Stripe catalog keys (shared Hartmann Studios billing worker, app "auracastweather")
        const val DIRECT_MONTHLY = "plus_monthly"
        const val DIRECT_YEARLY = "plus_yearly"
        const val DIRECT_PRICE_MONTHLY = "$2.99/month"
        const val DIRECT_PRICE_YEARLY = "$19.99/year"
    }

    private val _isAdFree = MutableStateFlow(false)
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _isAdFree.value = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        }
    }

    // Lazy: direct (non-Play) builds never create a Google Play BillingClient.
    private val client: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
    }

    // ── Direct (GitHub / charleshartmann.com) build: Stripe Checkout through DirectBilling instead of Google Play ──
    init {
        if (DirectBilling.isDirect) {
            DirectBilling.init(context, "auracastweather")
            DirectBilling.addListener { updateDirect() }
            updateDirect()
        }
    }

    private fun updateDirect() {
        _isAdFree.value = DirectBilling.isEntitled(DIRECT_MONTHLY, DIRECT_YEARLY)
    }

    /** Direct builds: Stripe Checkout for AuraCast Plus. */
    fun purchaseDirect(activity: Activity, productKey: String) {
        if (DirectBilling.isDirect) DirectBilling.purchase(activity, productKey)
    }

    /** Direct builds: look up a Stripe purchase by email (new phone, reinstall). */
    fun restoreDirect(activity: Activity) {
        if (DirectBilling.isDirect) DirectBilling.restore(activity)
    }

    /** Direct builds: Stripe customer portal. */
    fun manageDirect(activity: Activity) {
        if (DirectBilling.isDirect) DirectBilling.manageSubscription(activity)
    }

    private var connected = false

    private suspend fun ensureConnected(): Boolean {
        if (connected) return true
        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connected = result.responseCode == BillingClient.BillingResponseCode.OK
                    if (cont.isActive) cont.resume(connected)
                }
                override fun onBillingServiceDisconnected() {
                    connected = false
                }
            })
        }
    }

    suspend fun queryEntitlement(): Boolean {
        if (DirectBilling.isDirect) { updateDirect(); return _isAdFree.value }
        if (!ensureConnected()) return _isAdFree.value
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        val result = suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { _, purchases -> cont.resume(purchases) }
        }
        val entitled = result.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        _isAdFree.value = entitled
        return entitled
    }

    private suspend fun queryProductDetails(productId: String): ProductDetails? {
        if (DirectBilling.isDirect) return null
        if (!ensureConnected()) return null
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        return suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { _, result ->
                cont.resume(result.productDetailsList.firstOrNull())
            }
        }
    }

    suspend fun launchPurchaseFlow(activity: Activity, productId: String = PRODUCT_MONTHLY) {
        if (DirectBilling.isDirect) { purchaseDirect(activity, productId); return }
        val details = queryProductDetails(productId) ?: return
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        client.launchBillingFlow(activity, flowParams)
    }

    suspend fun restorePurchases(): Boolean = queryEntitlement()
}
