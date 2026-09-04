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

data class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val isPopular: Boolean = false,
)

/**
 * Phase 7 & 16 — On-device ML Kit Translation Manager.
 *
 * Utilizes Google's free on-device ML Kit translation packs (~30MB per language).
 * After a one-time download per language, works 100% offline with zero API keys or costs.
 *
 * Supports translating all dynamic and static UI strings across the application.
 */
@Singleton
class TranslationManager @Inject constructor() {
    private val modelManager = RemoteModelManager.getInstance()
    private val openTranslators = mutableMapOf<String, Translator>()
    private val translationCache = mutableMapOf<String, String>()
    private val cacheLock = Any()

    /**
     * Downloads and initializes the ML Kit translation model for [languageCode].
     * Defaults to allowing download over any connection (Wi-Fi or cellular) so user is not blocked.
     */
    suspend fun ensureModelDownloaded(languageCode: String, wifiOnly: Boolean = false): Boolean {
        if (languageCode == SOURCE_LANGUAGE) return true
        val model = TranslateRemoteModel.Builder(languageCode).build()
        val conditions = DownloadConditions.Builder()
            .apply { if (wifiOnly) requireWifi() }
            .build()

        val downloadResult = runCatching {
            modelManager.download(model, conditions).await()
            true
        }.getOrElse { false }

        if (downloadResult) {
            getOrCreateTranslator(SOURCE_LANGUAGE, languageCode)
            return true
        }

        // Fallback: try downloading via Translator client directly
        return runCatching {
            val translator = getOrCreateTranslator(SOURCE_LANGUAGE, languageCode)
            translator.downloadModelIfNeeded(conditions).await()
            true
        }.getOrElse { false }
    }

    fun isModelDownloaded(languageCode: String): Boolean {
        if (languageCode == SOURCE_LANGUAGE) return true
        return openTranslators.containsKey("$SOURCE_LANGUAGE->$languageCode")
    }

    suspend fun isModelDownloadedAsync(languageCode: String): Boolean {
        if (languageCode == SOURCE_LANGUAGE) return true
        val model = TranslateRemoteModel.Builder(languageCode).build()
        return runCatching { modelManager.isModelDownloaded(model).await() }.getOrDefault(false)
    }

    /**
     * Returns a synchronously cached translation if available, avoiding UI flicker during recompositions.
     */
    fun getCached(text: String, source: String = SOURCE_LANGUAGE, target: String): String? {
        if (text.isBlank() || source == target) return text
        val cacheKey = "$source->$target::$text"
        synchronized(cacheLock) {
            return translationCache[cacheKey]
        }
    }

    /**
     * Translates [text] from [source] to [target] using on-device ML Kit models.
     * Uses memory cache to guarantee sub-millisecond lookups for repeated strings.
     */
    suspend fun translate(text: String, source: String = SOURCE_LANGUAGE, target: String): String {
        if (text.isBlank()) return text
        if (source == target) return text

        val cacheKey = "$source->$target::$text"
        synchronized(cacheLock) {
            translationCache[cacheKey]?.let { return it }
        }

        val translator = getOrCreateTranslator(source, target)
        val result = runCatching {
            translator.translate(text).await()
        }.getOrElse {
            // Fallback to original text if model not ready or translation fails
            text
        }

        if (result.isNotBlank() && result != text) {
            synchronized(cacheLock) {
                if (translationCache.size > 2000) {
                    val toRemove = translationCache.keys.take(1000)
                    toRemove.forEach { translationCache.remove(it) }
                }
                translationCache[cacheKey] = result
            }
        }
        return result
    }

    private fun getOrCreateTranslator(source: String, target: String): Translator {
        val key = "$source->$target"
        return synchronized(cacheLock) {
            openTranslators.getOrPut(key) {
                Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(source)
                        .setTargetLanguage(target)
                        .build()
                )
            }
        }
    }

    fun clearCache() {
        synchronized(cacheLock) {
            translationCache.clear()
        }
    }

    suspend fun deleteModel(languageCode: String): Boolean {
        if (languageCode == SOURCE_LANGUAGE) return true
        val model = TranslateRemoteModel.Builder(languageCode).build()
        val deleted = runCatching {
            modelManager.deleteDownloadedModel(model).await()
            true
        }.getOrElse { false }

        synchronized(cacheLock) {
            val key = "$SOURCE_LANGUAGE->$languageCode"
            openTranslators.remove(key)?.close()
            translationCache.clear()
        }
        return deleted
    }

    fun close() {
        synchronized(cacheLock) {
            openTranslators.values.forEach { it.close() }
            openTranslators.clear()
            translationCache.clear()
        }
    }

    companion object {
        const val SOURCE_LANGUAGE = TranslateLanguage.ENGLISH

        /**
         * Comprehensive catalog of all 59 supported ML Kit on-device translation languages.
         */
        val SUPPORTED_LANGUAGES: List<AppLanguage> = listOf(
            AppLanguage("en", "English", "English", isPopular = true),
            AppLanguage("es", "Spanish", "Español", isPopular = true),
            AppLanguage("fr", "French", "Français", isPopular = true),
            AppLanguage("de", "German", "Deutsch", isPopular = true),
            AppLanguage("it", "Italian", "Italiano", isPopular = true),
            AppLanguage("pt", "Portuguese", "Português", isPopular = true),
            AppLanguage("zh", "Chinese", "中文", isPopular = true),
            AppLanguage("ja", "Japanese", "日本語", isPopular = true),
            AppLanguage("ko", "Korean", "한국어", isPopular = true),
            AppLanguage("ar", "Arabic", "العربية", isPopular = true),
            AppLanguage("hi", "Hindi", "हिन्दी", isPopular = true),
            AppLanguage("ru", "Russian", "Русский", isPopular = true),
            AppLanguage("tr", "Turkish", "Türkçe", isPopular = true),
            AppLanguage("nl", "Dutch", "Nederlands", isPopular = true),
            AppLanguage("pl", "Polish", "Polski", isPopular = true),
            AppLanguage("sv", "Swedish", "Svenska", isPopular = true),
            AppLanguage("uk", "Ukrainian", "Українська", isPopular = true),
            AppLanguage("vi", "Vietnamese", "Tiếng Việt", isPopular = true),
            AppLanguage("th", "Thai", "ไทย", isPopular = true),
            AppLanguage("id", "Indonesian", "Bahasa Indonesia", isPopular = true),
            AppLanguage("bn", "Bengali", "বাংলা", isPopular = true),
            AppLanguage("tl", "Tagalog (Filipino)", "Tagalog", isPopular = true),
            AppLanguage("el", "Greek", "Ελληνικά"),
            AppLanguage("cs", "Czech", "Čeština"),
            AppLanguage("da", "Danish", "Dansk"),
            AppLanguage("fi", "Finnish", "Suomi"),
            AppLanguage("he", "Hebrew", "עברית"),
            AppLanguage("hu", "Hungarian", "Magyar"),
            AppLanguage("no", "Norwegian", "Norsk"),
            AppLanguage("ro", "Romanian", "Română"),
            AppLanguage("sk", "Slovak", "Slovenčina"),
            AppLanguage("bg", "Bulgarian", "Български"),
            AppLanguage("hr", "Croatian", "Hrvatski"),
            AppLanguage("ca", "Catalan", "Català"),
            AppLanguage("lt", "Lithuanian", "Lietuvių"),
            AppLanguage("lv", "Latvian", "Latviešu"),
            AppLanguage("sl", "Slovenian", "Slovenščina"),
            AppLanguage("et", "Estonian", "Eesti"),
            AppLanguage("ga", "Irish", "Gaeilge"),
            AppLanguage("cy", "Welsh", "Cymraeg"),
            AppLanguage("is", "Icelandic", "Íslenska"),
            AppLanguage("ka", "Georgian", "ქართული"),
            AppLanguage("hy", "Armenian", "Հայերեն"),
            AppLanguage("sq", "Albanian", "Shqip"),
            AppLanguage("af", "Afrikaans", "Afrikaans"),
            AppLanguage("ms", "Malay", "Bahasa Melayu"),
            AppLanguage("eo", "Esperanto", "Esperanto"),
            AppLanguage("gl", "Galician", "Galego"),
            AppLanguage("ht", "Haitian Creole", "Kreyòl Ayisyen"),
            AppLanguage("mr", "Marathi", "मराठी"),
            AppLanguage("gu", "Gujarati", "ગુજરાતી"),
            AppLanguage("kn", "Kannada", "ಕನ್ನಡ"),
            AppLanguage("ta", "Tamil", "தமிழ்"),
            AppLanguage("te", "Telugu", "తెలుగు"),
            AppLanguage("ur", "Urdu", "اردو"),
            AppLanguage("fa", "Persian", "فارسی"),
            AppLanguage("sw", "Swahili", "Kiswahili"),
            AppLanguage("be", "Belarusian", "Беларуская"),
            AppLanguage("mk", "Macedonian", "Македонски"),
            AppLanguage("mt", "Maltese", "Malti"),
        )

        /** Launch languages for backward compatibility */
        val LAUNCH_LANGUAGES: List<Pair<String, String>> = SUPPORTED_LANGUAGES.map { it.code to it.nativeName }

        fun findLanguage(code: String): AppLanguage? =
            SUPPORTED_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }

        fun labelFor(code: String): String =
            findLanguage(code)?.nativeName ?: findLanguage(code)?.displayName ?: code

        fun englishLabelFor(code: String): String =
            findLanguage(code)?.displayName ?: code

        fun searchLanguages(query: String): List<AppLanguage> {
            if (query.isBlank()) return SUPPORTED_LANGUAGES
            val q = query.trim().lowercase()
            return SUPPORTED_LANGUAGES.filter {
                it.displayName.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
            }
        }
    }
}
