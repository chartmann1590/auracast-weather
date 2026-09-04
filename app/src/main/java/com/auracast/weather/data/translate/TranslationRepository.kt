package com.auracast.weather.data.translate

import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed class LanguageDownloadState {
    object Idle : LanguageDownloadState()
    object NotDownloaded : LanguageDownloadState()
    object Downloading : LanguageDownloadState()
    object Ready : LanguageDownloadState()
    data class Failed(val reason: String) : LanguageDownloadState()
    object RequiresWifi : LanguageDownloadState()
}

/**
 * App-wide translation repository — single source of truth for:
 * - selected language (backed by [TranslationPreferencesDataStore], survives process death)
 * - in-memory download status per language (so Settings/Onboarding can show spinner/check/error)
 * - cached translated strings via [TranslationManager] (free, on-device ML Kit)
 *
 * Every visible string in the app goes through [translate] (or the Compose helper
 * `rememberTranslated` / `TranslatedText` which delegates here) so selecting a native language in
 * onboarding instantly translates the whole app once the ML Kit pack finishes.
 */
@Singleton
class TranslationRepository @Inject constructor(
    private val manager: TranslationManager,
    private val prefs: TranslationPreferencesDataStore,
) {
    val currentLanguageFlow: Flow<String> = prefs.languageCodeFlow

    private val _downloadStates = MutableStateFlow<Map<String, LanguageDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, LanguageDownloadState>> = _downloadStates.asStateFlow()

    // Increments every time a pack finishes downloading or the language switches — lets
    // Compose helpers re-run translation even though `currentLanguage` hasn't changed but
    // `isModelReady` flipped from false → true in the background.
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    suspend fun currentLanguage(): String = prefs.languageCodeFlow.first()

    suspend fun currentLabel(): String = prefs.languageLabelFlow.first()

    /**
     * Fast synchronous check for already-translated strings in the in-memory cache.
     */
    fun getCachedTranslation(text: String, targetLanguage: String): String? {
        if (targetLanguage == TranslateLanguage.ENGLISH || text.isBlank()) return text
        return manager.getCached(text, TranslationManager.SOURCE_LANGUAGE, targetLanguage)
    }

    suspend fun setLanguage(code: String) {
        val label = TranslationManager.labelFor(code)
        prefs.setLanguage(code, label)
        manager.clearCache()
        _revision.value++
        // English never needs a model download — mark ready immediately.
        if (code == TranslateLanguage.ENGLISH) {
            _downloadStates.value = _downloadStates.value.toMutableMap().apply { this[code] = LanguageDownloadState.Ready }
            return
        }
        // For non-English, check if model already on disk so UI can show Ready without re-downloading.
        val already = manager.isModelDownloadedAsync(code)
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            this[code] = if (already) LanguageDownloadState.Ready else LanguageDownloadState.NotDownloaded
        }
    }

    /**
     * Ensures the ML Kit pack for [code] is downloaded. Tracks state in [downloadStates] so UI
     * can render downloading/ready/failed.
     */
    suspend fun ensureModelDownloaded(code: String, wifiOnly: Boolean = false): Boolean {
        if (code == TranslateLanguage.ENGLISH) return true
        val alreadyReady = _downloadStates.value[code] == LanguageDownloadState.Ready
        if (alreadyReady && manager.isModelDownloadedAsync(code)) return true

        _downloadStates.value = _downloadStates.value.toMutableMap().apply { this[code] = LanguageDownloadState.Downloading }
        val ok = manager.ensureModelDownloaded(code, wifiOnly = wifiOnly)
        if (ok) {
            manager.clearCache()
            _downloadStates.value = _downloadStates.value.toMutableMap().apply { this[code] = LanguageDownloadState.Ready }
            _revision.value++
            return true
        }

        val isWifiOnlyFailure = wifiOnly
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            this[code] = if (isWifiOnlyFailure) LanguageDownloadState.RequiresWifi else LanguageDownloadState.Failed("Download failed — check connection and try again.")
        }
        return false
    }

    suspend fun isModelReady(code: String): Boolean {
        if (code == TranslateLanguage.ENGLISH) return true
        _downloadStates.value[code]?.let { if (it == LanguageDownloadState.Ready) return true }
        return manager.isModelDownloadedAsync(code)
    }

    suspend fun deleteModel(code: String): Boolean {
        val ok = manager.deleteModel(code)
        if (ok) {
            manager.clearCache()
            _downloadStates.value = _downloadStates.value.toMutableMap().apply { this[code] = LanguageDownloadState.NotDownloaded }
            _revision.value++
        }
        return ok
    }

    /**
     * Translates [text] from English ([TranslationManager.SOURCE_LANGUAGE]) to the currently
     * selected language. Returns original text if target is English or model not yet downloaded.
     * Safe to call for every visible string — fast cache hit after first translation.
     */
    suspend fun translate(text: String): String {
        if (text.isBlank()) return text
        val target = currentLanguage()
        if (target == TranslateLanguage.ENGLISH) return text
        // If model isn't ready, show original so the user still sees something useful instead of blank.
        val ready = isModelReady(target)
        if (!ready) return text
        return manager.translate(text, TranslationManager.SOURCE_LANGUAGE, target)
    }

    suspend fun translateTo(text: String, target: String, source: String = TranslationManager.SOURCE_LANGUAGE): String {
        if (target == source) return text
        if (target == TranslateLanguage.ENGLISH) return text
        val ready = if (target == currentLanguage()) isModelReady(target) else manager.isModelDownloadedAsync(target)
        if (!ready) return text
        return manager.translate(text, source, target)
    }

    /** Refreshes download state for all supported languages — call on Settings entry so each row shows correct badge. */
    suspend fun refreshAllDownloadStates() {
        val states = mutableMapOf<String, LanguageDownloadState>()
        for (lang in TranslationManager.SUPPORTED_LANGUAGES) {
            val code = lang.code
            states[code] = when {
                code == TranslateLanguage.ENGLISH -> LanguageDownloadState.Ready
                manager.isModelDownloadedAsync(code) -> LanguageDownloadState.Ready
                else -> LanguageDownloadState.NotDownloaded
            }
        }
        // preserve any in-flight Downloading states so we don't flicker away a spinner
        val current = _downloadStates.value
        for ((k, v) in current) if (v == LanguageDownloadState.Downloading) states[k] = v
        _downloadStates.value = states
    }
}
