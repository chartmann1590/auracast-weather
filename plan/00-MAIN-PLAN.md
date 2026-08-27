# AuraCast Weather — Main Plan

> AI-narrated, on-device, ad-supported weather app for Android.
> Native Kotlin + Jetpack Compose. No paid weather API keys required at launch scale.

Every technical claim in this plan was verified against a live source in **August 2026** (see Sources at the bottom of this file and at the bottom of each phase file). Nothing here is guessed from training data — where training data and live search disagreed (Gemma 4's existence), the live, officially-hosted source (`ai.google.dev`) won.

---

## 1. What we're building

A single native Android app ("AuraCast Weather" — placeholder name, pick your own before Phase 13) that:

- Gets the user's location via Android's `FusedLocationProviderClient`, or lets them type a city/zip and geocode it.
- Shows **current conditions**, **hourly** (48h), **5-day**, and an animated **precipitation radar**, using **free, no-API-key** data sources.
- Generates a natural-language "weather report" using **Gemma 4** running **fully on-device** via **LiteRT-LM**, and reads it aloud with Android's on-device neural TTS — "AI weather podcast" experience.
- Translates its own UI into the user's language using **ML Kit Translation** (on-device, downloadable language packs).
- Sends **local notifications** (severe weather, daily summary) via `WorkManager` + Firebase Cloud Messaging (for future server-push alerts).
- Tracks stability/perf with **Firebase Crashlytics** + **Performance Monitoring**.
- Monetizes with **AdMob** banner + interstitial ads, and a **Play Billing v9** subscription to remove ads.
- Ships to the **Google Play Store**, source lives in a **private GitHub repo**, and has a companion **Firebase Hosting** marketing site with a privacy policy.

## 2. Confirmed tech stack (verified August 2026)

| Layer | Choice | Why / verification |
|---|---|---|
| Language / UI | Kotlin, Jetpack Compose, Compose BOM `2026.08.00` (Compose 1.12), Kotlin 2.2 compiler (compiler now ships inside the Kotlin repo) | [Android Developers Blog — Jetpack Compose August '26 release](https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html) |
| Current/hourly/daily weather | **Open-Meteo** REST API, no key | [open-meteo.com](https://open-meteo.com/) — free for **non-commercial** use (<10k calls/day, 5k/hr, 600/min); becomes commercial-use once the app carries ads/subscriptions — see §5 licensing note. [Pricing](https://open-meteo.com/en/pricing) · [Terms](https://open-meteo.com/en/terms) |
| US weather/alerts/radar | **api.weather.gov** (NWS/NOAA) | Free for any use, reasonable rate limits, GeoJSON. [weather-gov.github.io/api](https://weather-gov.github.io/api/) |
| International radar fallback | **RainViewer** public tile API | No key, no registration; free tier caps at zoom 7 / past-only frames / universal-blue color scheme; attribution requested. [rainviewer.com/api.html](https://www.rainviewer.com/api.html) |
| On-device LLM | **Gemma 4**, E2B/E4B variants, via **LiteRT-LM** (or Google AICore through MediaPipe AI Tasks on Android 12+) | Confirmed live on `ai.google.dev/gemma/docs/core/model_card_4`: released, sizes E2B/E4B/12B/26B‑A4B/31B, text+audio+image input, up to 256K context. E2B needs ~1.5GB RAM, E4B ~4GB. |
| On-device translation | **ML Kit Translation** | On-device, 58 languages, models download once then work fully offline, minSdk 23+. [developers.google.com/ml-kit/language/translation](https://developers.google.com/ml-kit/language/translation/) |
| Text-to-speech | Android `TextToSpeech` API using the on-device **Google Speech Services** neural voice pack | Google TTS defaults to server synthesis for top quality; switches to on-device neural synthesis once the user has downloaded an offline voice pack for their language — app must prompt for this (see Phase 6). |
| Notifications | `WorkManager` (scheduled/local) + **Firebase Cloud Messaging** (server push, future-proofing) | Standard Firebase Android stack. |
| Crash/perf monitoring | **Firebase Crashlytics** + **Firebase Performance Monitoring** | [firebase.google.com/docs/crashlytics](https://firebase.google.com/docs/crashlytics) |
| Ads | **Google Mobile Ads SDK (AdMob)** — adaptive banner + interstitial | Standard Kotlin AdMob integration. |
| Subscriptions | **Google Play Billing Library 9.1** | Current major version as of mid-2026; **all apps must be on v8+ by Aug 31, 2026** (extension to Nov 1, 2026 available). [Billing release notes](https://developer.android.com/google/play/billing/release-notes) · [RevenueCat: Billing 9.0](https://www.revenuecat.com/blog/engineering/play-billing-v9) |
| Website | **Firebase Hosting** (free tier: 10GB storage, 360MB/day transfer, auto HTTPS) | [firebase.google.com/products/hosting](https://firebase.google.com/products/hosting) |
| Source control | Private **GitHub** repo | User-managed, see Phase 14 |

## 3. High-level architecture

```
app/
  data/
    weather/          Open-Meteo + NWS repositories, DTOs, unit conversion
    radar/             RainViewer + NWS radar tile repositories
    location/          FusedLocationProviderClient wrapper + geocoding (Open-Meteo Geocoding API, no key)
    llm/               LiteRT-LM Gemma 4 session manager, prompt templates
    tts/               TextToSpeech wrapper, voice-pack availability checks
    translate/         ML Kit Translator manager, on-demand model downloads
  domain/              Use cases: GetCurrentWeather, GetForecast, GenerateAiReport, ...
  ui/
    home/              Current conditions + hourly strip + daily list (Compose)
    radar/              Animated radar map screen
    report/            "AI Weather Podcast" player screen
    settings/           Units, language, notifications, subscription management
  ads/                 AdMob banner/interstitial managers, ad-free gating
  billing/             Play Billing client, subscription state
  notifications/       WorkManager workers, FCM service
  monitoring/          Crashlytics + Performance Monitoring init
di/                    Hilt modules
```

**Data flow (typical "open app" path):**
1. `LocationRepository` resolves lat/lon (GPS or last-searched city).
2. `WeatherRepository` fires parallel Open-Meteo (or NWS, if US-resolved) requests for current/hourly/daily; results cached in Room for offline replay.
3. `RadarRepository` fetches RainViewer (or NWS radar) tile timeline.
4. UI renders immediately from cache/network as it streams in (no blocking spinner for the whole screen — each card loads independently).
5. On the "AI Report" tab, `GenerateAiReportUseCase` builds a prompt from the fetched weather JSON, runs it through the on-device Gemma 4 LiteRT-LM session, streams tokens into a script, and `TtsRepository` speaks it.

## 4. Phase index

Each phase is its own file in `plan/`, in build order. Phases 1–4 must ship before the app is minimally usable; 5–7 are the "wow factor" differentiators; 8–14 are production-readiness and business plumbing.

1. `01-phase-foundation-setup.md` — repo, Gradle, Compose scaffold, Hilt, CI skeleton
2. `02-phase-location-services.md` — GPS + manual location search/geocoding
3. `03-phase-weather-core-ui.md` — current conditions, hourly, 5-day, Open-Meteo/NWS integration
4. `04-phase-radar.md` — animated precipitation radar (RainViewer + NWS)
5. `05-phase-gemma4-llm.md` — on-device Gemma 4 via LiteRT-LM, AI weather report generation
6. `06-phase-tts-podcast.md` — natural on-device TTS "weather podcast" playback UI
7. `07-phase-mlkit-translation.md` — on-device app translation via ML Kit
8. `08-phase-notifications.md` — local + push notifications, severe weather alerts
9. `09-phase-firebase-monitoring.md` — Crashlytics + Performance Monitoring
10. `10-phase-monetization.md` — AdMob banner/interstitial + Play Billing subscription (ad-free)
11. `11-phase-polish-animations-icons.md` — icon set, motion design, dark mode, widgets
12. `12-phase-testing-release.md` — test strategy, Play Console listing, staged rollout
13. `13-phase-website-privacy-policy.md` — Firebase-hosted marketing site + privacy policy
14. `14-phase-github-repo-setup.md` — private repo structure, branch strategy, secrets handling
15. `15-rate-limits-and-quotas.md` — every external service's rate limit/quota, verified live, plus a licensing-risk summary
16. `16-design-system-and-visual-assets.md` — color/type/icon system, sourced+licensed illustration/animation assets, screen-by-screen flow spec

## 5. Cross-cutting decisions & risks

- **Open-Meteo commercial licensing**: Open-Meteo's free tier is explicitly for *non-commercial* use; running ads/subscriptions makes this app commercial. **Action item in Phase 3**: budget for Open-Meteo's paid "Standard" commercial plan (dedicated endpoint + key) before Play Store launch, or replace with 100%-free-for-commercial-use NWS data for US users and re-evaluate international coverage. Do not ship to production on the free tier while monetized.
- **RainViewer licensing is stricter than Open-Meteo's**: RainViewer's own docs state its free API is for **"personal and educational use only"** — not just "non-commercial." A monetized public app doesn't qualify. **Action item in Phase 4**: either negotiate a commercial license with RainViewer, or scope the animated radar screen to US locations only (NWS radar, free for any use) at launch and treat international radar as a post-launch item pending licensing. Full detail and every other service's exact limits are in `15-rate-limits-and-quotas.md`.
- **On-device TTS quality depends on the user having downloaded an offline voice pack.** The app must detect this (`TextToSpeech.getFeatures()` / voice `Voice.isNetworkConnectionRequired`) and deep-link the user to Settings → Accessibility → Text-to-speech output to download one, otherwise the "on-device podcast" silently falls back to network synthesis (still works, just not offline). Documented fully in Phase 6.
- **Gemma 4 device requirements**: E2B needs 3GB+ total device RAM; E4B needs 6GB+. Plan defaults to E2B with a Settings toggle to E4B on capable devices, and must handle "device not supported" gracefully (fall back to a template-based, non-AI report).
- **RainViewer free-tier limits** (max zoom 7, past-only, single color scheme) mean the radar screen should default to NWS radar for US-resolved locations and RainViewer everywhere else.
- **Billing Library 9 migration deadline** (Aug 31, 2026) is close — build against v9.1 from day one, don't start on an older version.

## 6. Sources (primary, fetched live during planning)

- Gemma 4 model card (official): https://ai.google.dev/gemma/docs/core/model_card_4
- Gemma 4 on-device/LiteRT-LM overview: https://developers.googleblog.com/bring-state-of-the-art-agentic-skills-to-the-edge-with-gemma-4/
- Open-Meteo pricing/terms: https://open-meteo.com/en/pricing · https://open-meteo.com/en/terms
- NWS API docs: https://weather-gov.github.io/api/
- RainViewer API docs: https://www.rainviewer.com/api.html
- ML Kit Translation (Android): https://developers.google.com/ml-kit/language/translation/android
- Play Billing release notes: https://developer.android.com/google/play/billing/release-notes
- Play Billing v9 overview (RevenueCat): https://www.revenuecat.com/blog/engineering/play-billing-v9
- Jetpack Compose August '26 release: https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html
- Firebase Crashlytics docs: https://firebase.google.com/docs/crashlytics
- Firebase Hosting product page: https://firebase.google.com/products/hosting
