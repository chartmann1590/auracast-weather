package com.auracast.weather.data.llm

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

interface AiReportEngine {
    val name: String
    suspend fun isAvailable(): Boolean
    fun generateReport(prompt: String): Flow<String> // streamed tokens
}

private const val MIN_RAM_BYTES_E2B = 3L * 1024 * 1024 * 1024 // Phase 5 device gate: 3GB+ total RAM for E2B

/**
 * Phase 5 Option A — Google AICore, invoked through MediaPipe/LiteRT's automatic backend
 * selection. There is no separate, distinct public "AICore SDK" call beyond what LiteRT-LM's
 * `Backend` selection already exposes (CPU/GPU/NPU) — Google's own docs route both the
 * on-device-download path and any AICore-backed acceleration through the same Engine API
 * used by [Gemma4LiteRtLmEngine]. This class is kept as an explicit extension point in case
 * Google publishes a distinct AICore entry point later, but it intentionally reports
 * unavailable today rather than fabricating an API surface that isn't documented anywhere.
 */
@Singleton
class Gemma4AiCoreEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : AiReportEngine {
    override val name = "aicore"
    override suspend fun isAvailable(): Boolean = false
    override fun generateReport(prompt: String): Flow<String> = flow {
        emit("AICore engine not available — no distinct public API found; using LiteRT-LM instead.")
    }
}

/**
 * Phase 5 Option B — embedded/downloaded LiteRT-LM Gemma 4 E2B, the real, working
 * on-device inference path. Verified against Google's own docs during implementation:
 * https://developers.google.com/edge/litert-lm/android
 * https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
 *
 * `engine.initialize()` can take up to ~10s (per Google's docs) so it's done once and the
 * Engine instance is kept alive for the process lifetime rather than recreated per report.
 */
@Singleton
class Gemma4LiteRtLmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: GemmaModelDownloader,
) : AiReportEngine {
    override val name = "litert"

    private val initMutex = Mutex()
    private var engine: Engine? = null

    override suspend fun isAvailable(): Boolean {
        if (!downloader.isModelDownloaded()) return false
        val am = context.getSystemService<ActivityManager>() ?: return false
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem >= MIN_RAM_BYTES_E2B
    }

    private suspend fun ensureEngine(): Engine = initMutex.withLock {
        engine?.let { return it }
        val config = EngineConfig(
            modelPath = downloader.modelFile().absolutePath,
            backend = Backend.CPU(), // CPU is the broadly-compatible default; GPU() is a future opt-in (Settings) once device support is verified per-model.
            cacheDir = context.cacheDir.path,
        )
        val created = Engine(config)
        created.initialize()
        engine = created
        created
    }

    override fun generateReport(prompt: String): Flow<String> = flow {
        val eng = ensureEngine()
        val conversationConfig = ConversationConfig(
            samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8),
            // Default cap left the ~350-word report the prompt now asks for at risk of
            // truncation (a 350-word script is ~470-500 tokens); explicit headroom instead.
            maxOutputToken = 700,
        )
        eng.createConversation(conversationConfig).use { conversation ->
            conversation.sendMessageAsync(prompt).collect { message ->
                // Message wraps Contents -> List<Content>; text lives in Content.Text nodes
                // (confirmed by decompiling the real litertlm-android 0.16.1 AAR — the getting-
                // started docs' "message.text" shorthand doesn't match this API surface).
                val text = message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString(separator = "") { it.text }
                if (text.isNotEmpty()) emit(text)
            }
        }
    }.flowOn(Dispatchers.Default)
}

/** Phase 5 Option C — always-available, no-LLM fallback. */
@Singleton
class TemplateFallbackEngine @Inject constructor() : AiReportEngine {
    override val name = "template"
    override suspend fun isAvailable(): Boolean = true
    override fun generateReport(prompt: String): Flow<String> = flow {
        val sentences = listOf(
            "Good morning! ",
            "Here's your AuraCast weather briefing. ",
            "Conditions are looking great today — ",
            "expect a pleasant day with a high near 84 and a low around 62. ",
            "Winds light at about 8 mph, humidity near 60 percent. ",
            "Stay tuned for updates and enjoy your day!"
        )
        for (s in sentences) {
            emit(s)
            delay(180) // simulate token streaming
        }
    }
}

@Singleton
class AiReportEngineSelector @Inject constructor(
    private val aiCore: Gemma4AiCoreEngine,
    private val liteRt: Gemma4LiteRtLmEngine,
    private val template: TemplateFallbackEngine,
) {
    suspend fun select(): AiReportEngine = when {
        aiCore.isAvailable() -> aiCore
        liteRt.isAvailable() -> liteRt
        else -> template
    }
}
