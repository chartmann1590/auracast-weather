package com.auracast.weather.data.translate

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.translationDataStore by preferencesDataStore(name = "translation_prefs")

@Singleton
class TranslationPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
    private val KEY_LANGUAGE_LABEL = stringPreferencesKey("selected_language_label")

    val languageCodeFlow: Flow<String> = context.translationDataStore.data.map { it[KEY_LANGUAGE_CODE] ?: "en" }
    val languageLabelFlow: Flow<String> = context.translationDataStore.data.map { it[KEY_LANGUAGE_LABEL] ?: "English" }

    suspend fun setLanguage(code: String, label: String) {
        context.translationDataStore.edit {
            it[KEY_LANGUAGE_CODE] = code
            it[KEY_LANGUAGE_LABEL] = label
        }
    }

    suspend fun setLanguageCode(code: String) {
        val label = TranslationManager.LAUNCH_LANGUAGES.firstOrNull { it.first == code }?.second ?: code
        setLanguage(code, label)
    }
}
