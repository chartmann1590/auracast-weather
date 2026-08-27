package com.auracast.weather.data.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

@Singleton
class OnboardingDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_COMPLETE = booleanPreferencesKey("onboarding_complete")

    val isCompleteFlow: Flow<Boolean> = context.onboardingDataStore.data.map { it[KEY_COMPLETE] ?: false }

    suspend fun markComplete() {
        context.onboardingDataStore.edit { it[KEY_COMPLETE] = true }
    }
}
