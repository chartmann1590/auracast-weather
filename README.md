# AuraCast Weather

**Your weather, narrated — fully on-device.**

AuraCast Weather is a native Android weather app that generates a spoken weather report using an on-device AI model (Gemma 4) and real neural text-to-speech, right on your phone. No account, no cloud AI calls for the report itself, and animated precipitation radar built in.

🌐 **Website:** https://auracast-weather.web.app
🔒 **Privacy Policy:** https://auracast-weather.web.app/privacy.html
📱 **Google Play:** coming soon

## Features

- **Current, 48-hour hourly, and 5-day forecast** (Open-Meteo + NWS)
- **AI weather podcast** — a natural-sounding daily report generated on-device with Gemma 4, read aloud with a real neural voice; pick and preview your favorite voice in Settings
- **Animated precipitation radar** (RainViewer + NWS tiles) with playback controls
- **On-device translation** into 58 languages via ML Kit — no server round-trip
- **Severe weather alerts & daily briefing** notifications
- **Light/dark themes**, no account required

## Privacy first

Your on-device AI report is generated entirely on your phone — forecast numbers are fed to a local model and the report never leaves your device. The only network calls are for fetching the weather forecast and radar imagery itself. See the full [Privacy Policy](https://auracast-weather.web.app/privacy.html) for details on what Crashlytics/Performance monitoring and ads collect.

## Tech stack

Native Android, Kotlin + Jetpack Compose, Hilt DI, Room, Coroutines/Flow. On-device inference via Google's LiteRT-LM (Gemma 4). Map rendering via osmdroid with a custom Coil-backed radar tile overlay. See `plan/` for the full build plan (16 phases).

## Building from source

```bash
./gradlew assembleDebug
./gradlew installDebug   # with a device/emulator connected
```

Firebase (`google-services.json`) is included for Crashlytics/Performance/Messaging. AdMob and Play Billing use test IDs in debug builds.

### Project structure

```
app/
  data/weather/     Open-Meteo + NWS
  data/radar/       RainViewer + NWS tiles
  data/location/    FusedLocationProviderClient + Open-Meteo Geocoding
  data/llm/         LiteRT-LM Gemma 4 session
  data/tts/         TextToSpeech wrapper + voice picker
  data/translate/   ML Kit Translation
  ui/home|radar|report|settings   Compose screens
  data/notifications  WorkManager + FCM
  data/monitoring     Crashlytics + Perf
  data/billing        Play Billing 9.1
website/            Landing page + privacy policy (Firebase Hosting)
```

## Support

Questions or issues? Email [support@auracast.app](mailto:support@auracast.app) or open a [GitHub issue](../../issues).

---

© 2026 AuraCast Weather · Built with Material 3, Meteocons (MIT), Material Symbols (Apache 2.0)
