package com.auracast.weather.data.tts

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ttsDataStore by preferencesDataStore(name = "tts_prefs")

@Singleton
class TtsPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_VOICE_NAME = stringPreferencesKey("selected_voice_name")

    val voiceNameFlow: Flow<String?> = context.ttsDataStore.data.map { it[KEY_VOICE_NAME] }

    suspend fun saveVoiceName(name: String) {
        context.ttsDataStore.edit { it[KEY_VOICE_NAME] = name }
    }
}
