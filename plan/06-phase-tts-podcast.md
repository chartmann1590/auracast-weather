# Phase 6 — Natural On-Device TTS "Weather Podcast" Playback

**Goal:** read the Gemma-4-generated script aloud in a natural neural voice, packaged as a "podcast player" UI, working fully offline once set up.

## TTS engine

- Use Android's built-in `android.speech.tts.TextToSpeech` API talking to the **Google Speech Services** engine (the default TTS engine on virtually all Android devices with Play Services).
- Confirmed behavior: Google's TTS engine synthesizes with neural voices; by default it **streams synthesis through Google's servers** for the best quality, and **switches to fully on-device processing** once the user has installed an **offline voice pack** for the target language (Settings → Accessibility → Text-to-speech output → Google Text-to-Speech Engine → Install voice data).
- Because this app's pitch is "on-device AI podcast," the app must actively drive the user toward the offline path rather than silently relying on network synthesis:

```kotlin
class TtsAvailability @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun offlineVoiceInstalled(locale: Locale): Boolean {
        val tts = awaitTtsInit(context)
        val voice = tts.voices?.firstOrNull { it.locale == locale }
        return voice != null && !voice.isNetworkConnectionRequired
    }

    fun openVoiceDownloadSettings(context: Context) {
        context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
    }
}
```

- On first use of the "AI Report" feature, if no offline voice is detected for the user's locale, show a one-time card: *"For fully offline AI weather podcasts, install an offline voice: Settings → Accessibility → Text-to-Speech → \[Language\] → Download."* with a button that opens `TTS_SETTINGS` directly. The feature still works over network TTS in the meantime — never block the feature, just nudge toward the better path.

## Player UX ("podcast" framing)

- Dedicated **Report tab**: album-art-style animated weather icon (see Phase 11) in place of podcast cover art, script text scrolling karaoke-style in sync with `TextToSpeech.setOnUtteranceProgressListener` range callbacks (`onRangeStart(utteranceId, start, end, frame)` — available since API 26, matches our minSdk), play/pause/scrub controls styled like a media player, using `MediaSession`/`MediaStyle` notification so it behaves like real audio media (lock-screen controls, Bluetooth/headset play-pause).
- Speech parameters: `setSpeechRate(1.0f)` default with a 0.75x–1.5x slider (persisted per-user), `setPitch(1.0f)`, select the most natural available voice for the locale via `Voice.getQuality()` (prefer `QUALITY_VERY_HIGH`/`HIGH` network-optional voices).
- Chunk long scripts into per-sentence utterances queued with `QUEUE_ADD` so playback can pause/resume/seek at sentence granularity rather than only "stop entirely."
- Cache the last-generated report + a flag for whether it was fully on-device, so re-opening the tab replays instantly without re-invoking Gemma 4 unless the user pulls to refresh.

## Error handling

- `TextToSpeech.OnInitListener` failure (`ERROR` status) → show "Text-to-speech isn't available on this device" and hide the Report tab's play button (report text still shown, readable).
- Missing locale voice entirely → fall back to the device's default TTS locale and prepend a small "(read in English — your language isn't installed for speech yet)" notice, rather than silently mismatching language.

## Acceptance criteria
- With an offline voice pack installed and airplane mode on, tapping play produces natural-sounding audio with zero network activity.
- Without an offline pack, playback still works (via network synthesis) and the install nudge is shown exactly once per language, not on every visit.
- Lock-screen media controls (play/pause/skip) work correctly during playback.

## Sources
- Google TTS on-device/offline voice pack behavior verified via Android accessibility documentation and current TTS coverage: https://support.google.com/accessibility/android/answer/6006983 and https://www.topvox.ai/blog/google-text-to-speech-review/
- `TextToSpeech` / `UtteranceProgressListener` are standard `android.speech.tts` framework APIs available since the platform versions this app already targets (minSdk 26).
