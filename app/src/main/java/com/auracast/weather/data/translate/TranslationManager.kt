package com.auracast.weather.data.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 7 — ML Kit Translation, fully on-device once a language pack is downloaded.
 * Verified against https://developers.google.com/ml-kit/language/translation/android
 * (58 languages, ~30MB/pack, offline after first download, minSdk 23+ — we're 26+).
 *
 * App-wide on-device translation (Phase 7 + Phase 16): [TranslationPreferencesDataStore]
 * holds the user's selected native language (set during onboarding, changeable in
 * Settings). [TranslationRepository] wraps this manager with caching + prefs so every
 * visible string can be run through [translate] and will show in the chosen language
 * once its pack is downloaded — free, offline, no API key.
 */
@Singleton
class TranslationManager @Inject constructor() {
    private val modelManager = RemoteModelManager.getInstance()
    private val openTranslators = mutableMapOf<String, Translator>()
    // In-memory translation cache — avoids re-hitting the ML Kit native engine for
    // the same (source,target,text) triple, which matters because every screen now
    // runs dozens of strings through translate() on each recomposition after a language switch.
    private val translationCache = mutableMapOf<String, String>()
    private val cacheLock = Any()

    suspend fun ensureModelDownloaded(languageCode: String, wifiOnly: Boolean = true): Boolean {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        val conditions = DownloadConditions.Builder()
            .apply { if (wifiOnly) requireWifi() }
            .build()
        return runCatching { modelManager.download(model, conditions).await(); true }
            .getOrElse { false }
    }

    fun isModelDownloaded(languageCode: String): Boolean {
        // ML Kit's own check is async (Task<Boolean>); callers that need a synchronous
        // read should collect isModelDownloadedFlow-style state elsewhere. This overload
        // exists for call-site symmetry with ensureModelDownloaded/deleteModel.
        return openTranslators.containsKey(languageCode)
    }

    suspend fun isModelDownloadedAsync(languageCode: String): Boolean {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        return runCatching { modelManager.isModelDownloaded(model).await() }.getOrDefault(false)
    }

    suspend fun translate(text: String, source: String, target: String): String {
        if (text.isBlank()) return text
        if (source == target) return text
        val cacheKey = "$source->$target::$text"
        synchronized(cacheLock) { translationCache[cacheKey]?.let { return it } }
        val key = "$source->$target"
        val translator = openTranslators.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build()
            )
        }
        val result = runCatching { translator.translate(text).await() }.getOrDefault(text)
        synchronized(cacheLock) {
            // Simple bounded cache — evict oldest half when it grows too large (~500 entries ~ trivial memory)
            if (translationCache.size > 500) {
                val toRemove = translationCache.keys.take(250)
                toRemove.forEach { translationCache.remove(it) }
            }
            translationCache[cacheKey] = result
        }
        return result
    }

    /** Clears the in-memory translation cache — call when the user switches language so stale entries aren't reused. */
    fun clearCache() {
        synchronized(cacheLock) { translationCache.clear() }
    }

    suspend fun deleteModel(languageCode: String): Boolean {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        return runCatching { modelManager.deleteDownloadedModel(model).await(); true }.getOrElse { false }
    }

    fun close() {
        openTranslators.values.forEach { it.close() }
        openTranslators.clear()
    }

    /**
     * Expanded launch list — still curated to keep the onboarding grid readable, but now also
     * covers all free ML Kit packs a user might reasonably pick as their native language.
     * Add more here when we expand; any TranslateLanguage.* constant (≈58) will work as long
     * as strings are still run through [translate] at display time.
     */
    companion object {
        /** Curated launch language list (Phase 16 onboarding picker) — BCP-47-ish codes ML Kit accepts. */
        val LAUNCH_LANGUAGES: List<Pair<String, String>> = listOf(
            TranslateLanguage.ENGLISH to "English",
            TranslateLanguage.SPANISH to "Español",
            TranslateLanguage.FRENCH to "Français",
            TranslateLanguage.GERMAN to "Deutsch",
            TranslateLanguage.PORTUGUESE to "Português",
            TranslateLanguage.ITALIAN to "Italiano",
            TranslateLanguage.DUTCH to "Nederlands",
            TranslateLanguage.RUSSIAN to "Русский",
            TranslateLanguage.KOREAN to "한국어",
            TranslateLanguage.HINDI to "हिन्दी",
            TranslateLanguage.CHINESE to "中文",
            TranslateLanguage.JAPANESE to "日本語",
            TranslateLanguage.ARABIC to "العربية",
            TranslateLanguage.TURKISH to "Türkçe",
            TranslateLanguage.POLISH to "Polski",
            TranslateLanguage.SWEDISH to "Svenska",
            TranslateLanguage.UKRANIAN to "Українська",
            TranslateLanguage.VIETNAMESE to "Tiếng Việt",
            TranslateLanguage.THAI to "ไทย",
            TranslateLanguage.INDONESIAN to "Indonesia",
        )

        fun labelFor(code: String): String = LAUNCH_LANGUAGES.firstOrNull { it.first == code }?.second ?: code

        const val SOURCE_LANGUAGE = TranslateLanguage.ENGLISH
    }
}
