# Phase 5 — On-Device Gemma 4 AI Weather Report

**Goal:** generate a natural-language, personality-driven weather report entirely on-device using **Gemma 4**, no cloud LLM call, no per-request cost.

This is the headline differentiator. Verified live against Google's official docs during planning (not from training-data memory): Gemma 4 is a real, released, open-weight model family (Apache 2.0), sizes **E2B / E4B / 12B / 26B-A4B / 31B**, multimodal (text, image, and audio input on the smaller sizes), up to 256K context, and it runs on-device via **LiteRT-LM**. Source: https://ai.google.dev/gemma/docs/core/model_card_4

## Deployment options (all confirmed for Android)

| Option | How it works | Trade-off |
|---|---|---|
| **A. Google AICore (recommended default)** | On Android 12+ devices with Play Services, the model downloads once in the background via AICore and is invoked through the **MediaPipe AI Tasks** GenAI LLM Inference API. App ships with zero model weight bytes in the APK. | Smallest APK, Google manages updates/optimization; requires AICore-eligible device — must degrade gracefully on devices without it. |
| **B. Embedded LiteRT-LM** | Ship the Gemma 4 E2B `.litertlm` weights (from `litert-community/gemma-4-E2B-it-litert-lm` on Hugging Face) inside the APK or as a Play Asset Delivery on-demand module, run through the LiteRT-LM runtime directly. | Fully self-contained, works offline immediately, works on any device meeting RAM floor — but adds ~1.5GB+ to install size, so must use **Play Asset Delivery (on-demand/install-time asset pack)**, not a bundled APK asset. |
| **C. AI Edge Gallery reference app** | Google's own demo app for trying Gemma 4 on-device. | Not for production use — reference only, to validate device behavior during development. |

**Decision:** use **Option A (AICore/MediaPipe) as the primary path**, with **Option B (embedded LiteRT-LM E2B via Play Asset Delivery) as the fallback** for devices/regions where AICore isn't available. Both paths present the same app-level interface (`AiReportEngine`), so the app logic doesn't care which backend actually ran.

## Device gating

- E2B: minimum 3GB total device RAM, Android 12+ for AICore path.
- E4B (optional, Settings toggle "Higher quality reports"): minimum 6GB RAM.
- Below the RAM floor, or on Android <12 without AICore: fall back to a **template-based report** (string-interpolated sentences from the structured forecast data, no LLM) so the feature never simply disappears — it degrades to "still useful, less flavorful."

```kotlin
interface AiReportEngine {
    suspend fun isAvailable(): Boolean
    suspend fun generateReport(prompt: WeatherPromptContext): Flow<String> // streamed tokens
}

class Gemma4AiCoreEngine @Inject constructor(...) : AiReportEngine { /* MediaPipe LLM Inference API */ }
class Gemma4LiteRtLmEngine @Inject constructor(...) : AiReportEngine { /* direct LiteRT-LM session */ }
class TemplateFallbackEngine @Inject constructor(...) : AiReportEngine { /* no LLM */ }

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
```

## Prompt design

Build a compact, structured context block from the already-fetched `WeatherSnapshot` (Phase 3) — don't let the model hallucinate numbers, feed them in:

```
System: You are a friendly, upbeat local weather reporter recording a short
daily podcast segment. Use the exact numbers given. Keep it under 120 words.
Speak naturally, like a radio host, not like a data table.

Context:
Location: Austin, TX
Current: 84°F, feels like 88°F, partly cloudy, humidity 61%, wind 8mph SW
Today's high/low: 91°F / 72°F
Hourly trend: rising to 91°F by 3pm, chance of storms after 5pm (60%)
Tomorrow: 89°F / 71°F, mostly sunny

Task: Write the podcast script now.
```

- Use **constrained decoding** (a LiteRT-LM feature per the researched docs) to keep numeric fields from drifting, if the SDK exposes it for this use case; otherwise a light post-generation regex/number-consistency check against the source data, discarding+retrying once if numbers don't match.
- Stream tokens into the UI as a "typing" script preview while the audio (Phase 6) is prepared, so users see something within ~1s instead of waiting for the whole generation.

## Performance targets (from research)

- E4B on a Pixel 8 Pro–class device: ~15–30 tok/s.
- E2B on a mid-range device: ~5–15 tok/s.
- A ~120-word script (~160 tokens) should therefore complete in well under 15s even on the slower path — acceptable for a "loading the podcast" UX with a spinner/skeleton.

## Acceptance criteria
- On an AICore-eligible device, tapping "Generate Report" produces a coherent, factually-consistent script referencing the real current numbers within 15s, fully offline (airplane mode after data fetch) except for the initial model download.
- On a low-RAM/old-Android test device, the same button produces a template-based report instead of crashing or hanging.
- No network call is made during generation itself (verifiable via network inspector — only the one-time model download traffic exists).

## Sources
- Gemma 4 official model card (release, sizes, context window, modalities): https://ai.google.dev/gemma/docs/core/model_card_4
- Gemma 4 edge/on-device blog post (LiteRT-LM, AICore, memory footprint, constrained decoding): https://developers.googleblog.com/bring-state-of-the-art-agentic-skills-to-the-edge-with-gemma-4/
- LiteRT-LM community weights for Android: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm and https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm
