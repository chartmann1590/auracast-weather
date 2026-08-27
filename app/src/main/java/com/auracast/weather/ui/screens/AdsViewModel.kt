package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import com.auracast.weather.data.ads.AdManager
import com.auracast.weather.data.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Thin Hilt entry point so Composables can reach [AdManager]/[BillingManager] via hiltViewModel(). */
@HiltViewModel
class AdsViewModel @Inject constructor(
    val adManager: AdManager,
    val billingManager: BillingManager,
) : ViewModel()
