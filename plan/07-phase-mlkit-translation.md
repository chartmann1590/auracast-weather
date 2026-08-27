# Phase 7 — On-Device App Translation via ML Kit

**Goal:** let users switch the app's language (UI strings *and* the AI-generated report) using ML Kit's on-device Translation API — no cloud call, no key.

## Confirmed facts
- ML Kit Translation runs fully on-device, covers **58 languages**, uses the same models as Google Translate's offline mode, requires **minSdk 23+** (already satisfied — our floor is 26).
- Language models are **not bundled**; each language pack downloads on first use (over network) and is then cached locally for fully offline use afterward.
- Source: https://developers.google.com/ml-kit/language/translation/android and usage guidelines at https://developers.google.com/ml-kit/language/translation/translation-terms

## Dependency

```kotlin
implementation("com.google.mlkit:translate:17.0.3") // pin to latest at implementation time
```

## What gets translated

1. **AI report script (Phase 5/6 output)** — the highest-value case, since Gemma 4 generates in whatever language it defaults to (English) unless prompted otherwise. Two approaches, pick based on testing:
   - **(a) Prompt Gemma 4 directly in the target language** (it supports 140+ languages per the model card) — better fluency, no extra translation pass, preferred default.
   - **(b) Generate in English, then run ML Kit Translation as a fixed pass** — useful as a guaranteed-consistent fallback if a given target language produces weaker direct generations, and lets TTS Phase 6 pick a locale-matched voice deterministically.
   - Plan: try (a) first per language; if internal QA finds a language where direct generation is weak, flip that language to path (b) via a per-locale config flag.
2. **Static UI strings** — standard Android `strings.xml` localization (`values-es/strings.xml`, etc.) for the top ~10 languages at launch (translate these normally, not via ML Kit — ML Kit is for dynamic content, not app resource strings). ML Kit Translation is reserved for **dynamic/generated text** (the report script, and optionally condition descriptions from the weather API if a given source only returns English text).

## Model manager UX

```kotlin
class TranslationManager @Inject constructor() {
    private val modelManager = RemoteModelManager.getInstance()

    suspend fun ensureModelDownloaded(language: TranslateLanguage, wifiOnly: Boolean): Boolean {
        val model = TranslateRemoteModel.Builder(language).build()
        val conditions = DownloadConditions.Builder()
            .apply { if (wifiOnly) requireWifi() }
            .build()
        return runCatching { modelManager.download(model, conditions).await(); true }
            .getOrElse { false }
    }

    fun translator(source: String, target: String): Translator =
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
        )
}
```

- Settings screen: language picker showing download state per language (Not downloaded / Downloading / Ready), a "Wi-Fi only downloads" toggle (default on, respects user data plans), and a "Manage downloaded languages" list with per-language delete to reclaim storage.
- Always call `translator.close()` when no longer needed (ML Kit `Translator` instances hold native resources).

## Acceptance criteria
- Switching the in-app language to Spanish downloads the model once (visible progress), then works with airplane mode on for all subsequent report generations.
- Deleting a downloaded language frees the storage and correctly falls back to English (or re-prompts download) if selected again.
- No app strings ever call ML Kit for static UI text — verified by code review, ML Kit calls only appear in the report-generation path.

## Sources
- ML Kit Translation Android guide: https://developers.google.com/ml-kit/language/translation/android
- ML Kit Translation overview (58 languages, offline mode parity): https://developers.google.com/ml-kit/language/translation/
- Usage guidelines: https://developers.google.com/ml-kit/language/translation/translation-terms
