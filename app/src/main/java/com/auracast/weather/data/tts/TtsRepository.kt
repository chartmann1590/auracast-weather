package com.auracast.weather.data.tts

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class VoiceOption(
    val name: String,
    val localeDisplay: String,
    val quality: Int, // Voice.QUALITY_* — higher is better
    val offline: Boolean,
)

@Singleton
class TtsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: TtsPreferencesDataStore,
) {
    private var tts: TextToSpeech? = null
    private var initialized = false
    private var listenerAttached = false

    // Phase 6 fix — previously a *new* UtteranceProgressListener was registered on every
    // sentence in speak(), but TextToSpeech only ever holds a single global listener, so all
    // but the last one were silently discarded and progress jumped straight to 100%. One
    // listener is now attached once and tracks the whole queued utterance list below.
    private var utteranceQueue: List<String> = emptyList()
    private var onProgress: ((Float) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null

    private val sharedListener = object : UtteranceProgressListener() {
        override fun onStart(id: String?) {}
        override fun onDone(id: String?) = reportProgress(id)
        @Deprecated("Deprecated in Java")
        override fun onError(id: String?) = reportProgress(id)
        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {}

        private fun reportProgress(id: String?) {
            val idx = utteranceQueue.indexOf(id)
            if (idx < 0) return
            onProgress?.invoke((idx + 1).toFloat() / utteranceQueue.size)
            if (idx == utteranceQueue.size - 1) onComplete?.invoke()
        }
    }

    private suspend fun ensureInit(): TextToSpeech {
        tts?.let { if (initialized) return it }
        var created: TextToSpeech? = null
        val result = suspendCancellableCoroutine<TextToSpeech> { cont ->
            created = TextToSpeech(context) { status ->
                initialized = status == TextToSpeech.SUCCESS
                created?.let { inst ->
                    tts = inst
                    if (cont.isActive) cont.resume(inst)
                }
            }
        }
        if (!listenerAttached) {
            result.setOnUtteranceProgressListener(sharedListener)
            listenerAttached = true
        }
        return result
    }

    /** Voices for the specified or device's current language, deduped, best quality first. */
    suspend fun getAvailableVoices(languageCode: String? = null): List<VoiceOption> {
        val engine = ensureInit()
        if (!initialized) return emptyList()
        val lang = languageCode ?: Locale.getDefault().language
        return engine.voices.orEmpty()
            .filter { it.locale.language == lang && !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .distinctBy { it.name }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.isNetworkConnectionRequired })
            .map {
                VoiceOption(
                    name = it.name,
                    localeDisplay = it.locale.displayName,
                    quality = it.quality,
                    offline = !it.isNetworkConnectionRequired,
                )
            }
    }

    val selectedVoiceNameFlow: Flow<String?> = prefs.voiceNameFlow

    suspend fun selectVoice(voiceName: String) {
        prefs.saveVoiceName(voiceName)
        applySelectedVoice()
    }

    private suspend fun applySelectedVoice() {
        val engine = ensureInit()
        if (!initialized) return
        val name = prefs.voiceNameFlow.firstOrNull() ?: return
        engine.voices?.firstOrNull { it.name == name }?.let { engine.voice = it }
    }

    suspend fun previewVoice(voiceName: String) {
        val engine = ensureInit()
        if (!initialized) return
        engine.voices?.firstOrNull { it.name == voiceName }?.let { engine.voice = it }
        engine.speak(
            "Hi there! This is a preview of how I'll sound reading your weather report.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "preview-${System.currentTimeMillis()}",
        )
    }

    suspend fun isOfflineVoiceAvailable(locale: Locale = Locale.getDefault()): Boolean {
        return try {
            val engine = ensureInit()
            val voice: Voice? = engine.voices?.firstOrNull { it.locale.language == locale.language }
            voice != null && !voice.isNetworkConnectionRequired
        } catch (_: Exception) { false }
    }

    fun openVoiceDownloadSettings(ctx: Context) {
        try {
            ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {
            ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    }

    suspend fun speak(text: String, languageCode: String? = null, onProgress: (Float) -> Unit = {}, onComplete: () -> Unit = {}) {
        val engine = ensureInit()
        if (!initialized) return
        if (languageCode != null) {
            runCatching { engine.language = Locale(languageCode) }
        }
        applySelectedVoice()

        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        if (sentences.isEmpty()) {
            onComplete()
            return
        }
        this.onProgress = onProgress
        this.onComplete = onComplete
        utteranceQueue = sentences.indices.map { "utt-$it" }
        sentences.forEachIndexed { idx, sentence ->
            engine.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceQueue[idx])
        }
    }

    fun stop() {
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun seek(fraction: Float) { /* Phase 6: re-queue from sentence index — not yet implemented */ }
}
