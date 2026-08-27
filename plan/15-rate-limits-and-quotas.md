# Phase 15 — Rate Limits & Quotas Reference (every external dependency)

This file is the single source of truth for every rate limit, quota, and usage restriction the app is subject to. Every number below was pulled from the provider's own live documentation in August 2026 (not estimated). Cross-reference this file whenever a phase's implementation touches one of these services — the individual phase files link back here rather than duplicating numbers that can drift.

## 1. Open-Meteo (Forecast + Geocoding APIs) — Phase 2, Phase 3

| Window | Limit |
|---|---|
| Per minute | 600 calls |
| Per hour | 5,000 calls |
| Per day | 10,000 calls |
| Per month | 300,000 calls |

- These limits apply **per source IP**, and cover *all* Open-Meteo endpoints combined (Forecast, Geocoding, Air Quality, etc.) since they share one quota.
- Monthly limit is soft: "monthly limits are not enforced" yet as of this writing — a usage portal is "under development" — do not rely on this staying unenforced; build as if 300k/month is a hard wall.
- Email alerts fire automatically at 80%/90%/100% of the monthly budget **to the account owner**, not to the app — you must monitor this yourself if usage climbs.
- **Free tier = non-commercial only.** Ads + subscriptions in this app make it commercial. **This is a launch blocker**, not a nice-to-have fix — see §6 of the main plan and the Licensing Risk Summary below.
- **App-side mitigation regardless of tier**: client-side caching (Phase 3, 15-min current / 1-hr hourly-daily TTL) means each *device* makes at most a handful of calls per hour, not per screen-open — the quota is a shared backend budget across your entire user base hitting Open-Meteo's own servers if you proxy nothing, so at real scale you will need either the commercial plan or your own caching proxy layer in front of Open-Meteo (see mitigation option below).
- Source: https://open-meteo.com/en/pricing

## 2. National Weather Service — `api.weather.gov` — Phase 3, Phase 4 (radar), Phase 8 (alerts)

| Window | Limit |
|---|---|
| Documented numeric limit | **None published.** NWS does not state a requests-per-second/day ceiling. |
| Enforcement | Undocumented rate-limiting firewall exists; abusive traffic gets throttled/blocked. Their own alerts-service guidance: don't poll more than once every ~30 seconds. |
| Required header | `User-Agent` identifying your app + a contact method (e.g. `"AuraCastWeather/1.0 (contact@yourdomain.com)"`) — **not optional**, undocumented/missing User-Agent risks being treated as abusive traffic. |
| Caching | The API is built to support standard HTTP caching (`Cache-Control`, `Last-Modified`/`ETag`). **Do not** append random/cache-busting query params — this triggers `400 Bad Request`. Respect the cache headers the API returns instead of polling on your own arbitrary interval. |
| Cost | Free for any use, including commercial — no tier restriction. |

- **App-side design implication**: Phase 8's severe-alert `WorkManager` worker already runs on a 15-minute floor (OS-imposed minimum for periodic work), which comfortably respects the ~30s NWS guidance — no change needed, just don't be tempted to shorten it.
- Source: https://weather-gov.github.io/api/general-faqs

## 3. RainViewer (radar tiles, international fallback) — Phase 4

| Restriction | Detail |
|---|---|
| Stated permitted use | **"The API is free for personal or educational use."** / **"This API is available for personal and educational use only."** — quoted directly from their own docs. |
| Numeric rate limit | None officially published on the primary docs page. Some third-party aggregators report an unofficial ~1,000 requests/day/key figure, but this is **not confirmed on RainViewer's own site** — treat it as folklore, not a contract. |
| Enforcement | No documented rate limiter; implied "cache aggressively, don't hammer, or risk IP block." |
| Free-tier feature caps | Max zoom level 7 (256/512px tiles), Universal-Blue color scheme only, past-radar frames only (no forecast/nowcast, no satellite) — these are feature restrictions, not rate limits, already noted in Phase 4. |
| Attribution | Required: visible "Radar © RainViewer.com" credit linking to rainviewer.com. |

- **⚠️ Licensing risk — read this before building Phase 4 for non-US users.** RainViewer's own page states its free tier is for **personal and educational use only** — it does not say "non-commercial" (which some apps could argue around), it says personal/educational, which an ad-and-subscription-monetized public Play Store app is neither. This is a stricter and clearer restriction than Open-Meteo's. **Action item**: before launch, either (a) contact RainViewer directly about a commercial/paid license for the international-radar use case, or (b) restrict the animated radar screen to US locations only (NWS radar, which has no such restriction) and hide/omit the radar tab for non-US users at launch, expanding later once a licensed international radar source is contracted. Do not ship international RainViewer radar into a monetized production build without resolving this.
- Source: https://www.rainviewer.com/api.html

## 4. ML Kit Translation (on-device) — Phase 7

| Item | Detail |
|---|---|
| API rate limit | None — models, once downloaded, run **entirely on-device**; there's no server call per-translation to rate-limit. |
| Per-language model download size | ~30MB per language pack. |
| Download guidance (from Google) | Only download over Wi-Fi unless the user opts in to cellular; don't download languages speculatively; delete unused models to reclaim storage. |
| Cost | Free, unlimited, no key, at any usage volume — it's a local SDK feature, not a metered cloud API. |

- Source: https://developers.google.com/ml-kit/language/translation/android

## 5. Gemma 4 on-device (AICore / LiteRT-LM) — Phase 5

| Item | Detail |
|---|---|
| Per-inference rate limit | None — inference runs locally on the device's NPU/GPU/CPU; nothing to throttle. |
| One-time cost | Model download bandwidth (once per device): E2B and E4B `.litertlm` weights are multi-hundred-MB to low-GB depending on quantization — budget for this in the "first run" UX (Wi-Fi prompt, progress bar), same spirit as the ML Kit guidance above. |
| AICore path | Google manages background delivery via Play Services; no app-level quota to track. |

- No numeric quota exists to document here beyond what's already covered in Phase 5's device-RAM gating — included for completeness since this is the app's core differentiator and worth stating explicitly that it's quota-free.

## 6. Firebase (Spark free plan) — Phase 9, Phase 13

| Service | Free (Spark) limit |
|---|---|
| Crashlytics | **Unlimited, no-cost**, regardless of volume. |
| Performance Monitoring | **Unlimited, no-cost**, regardless of volume. |
| Cloud Messaging (FCM) | **Unlimited, no-cost**, regardless of volume. |
| Hosting | **10 GB stored**, **360 MB/day** data transfer (confirmed directly on firebase.google.com/pricing). |

- The 360MB/day Hosting transfer cap is the only one of these that can realistically be hit — a simple marketing site with a handful of screenshots/GIFs should stay well under it, but keep hero media compressed (WebP/AVIF, not uncompressed PNG/MP4) and monitor the Firebase console's Hosting usage tab after any traffic spike (e.g. a Play Store feature or a viral post).
- If the app later adds Firestore/Realtime Database (not currently in this plan's scope), those have their own separate Spark quotas (1 GiB storage, 50k reads/day, 20k writes/day for Firestore) — not relevant until/unless a backend is added.
- Source: https://firebase.google.com/pricing

## 7. AdMob — Phase 10

| Item | Detail |
|---|---|
| Client-side ad request limit (banner/interstitial via Mobile Ads SDK) | **No published numeric limit.** Google instead governs this via *behavioral policy* — invalid-traffic detection, ad-serving limits during a "new app readiness review" period, and account-level throttling if quality/fraud signals are poor. There is no fixed "N requests per minute" number to design against. |
| Ad refresh rate | Must stay within the range the Mobile Ads SDK itself allows for banner refresh — don't hand-roll a refresh interval outside SDK-supported bounds. |
| AdMob **reporting/management API** (server-side, only relevant if you build automated reporting tooling later — not needed for the MVP) | Account reads: 900/min/project · Inventory reads: 120/min/project, 172,800/day/project · Reporting reads: 900/min/project. |
| New-app review | New AdMob apps go through an automatic "app readiness review" during which ad serving may be intentionally limited — expect lower fill/impressions in the first days after launch; this is normal, not a bug. |

- **Design implication for Phase 10**: because there's no fixed client-side quota, the interstitial cadence caps this plan already set (max once per session, 3-minute minimum gap) exist for *retention/UX* reasons, not because of a rate limit — don't remove them thinking "AdMob allows more," the removal risk is user churn and policy strikes for aggressive ad behavior, not a technical throttle.
- Source: https://developers.google.com/admob/api/quotas · https://support.google.com/admob/answer/9493252

## 8. Google Play Billing — Phase 10

| Item | Detail |
|---|---|
| **On-device `BillingClient` calls** (`queryPurchasesAsync`, `launchBillingFlow`, etc.) | No published client-SDK rate limit — these are local calls to Play Store services on-device, not metered network API calls from the app's perspective. |
| **Google Play Developer API** (server-side; only relevant if/when a backend does server-side purchase verification — not required for MVP per Phase 10's YAGNI call) | Default quota: **3,000 queries/minute per bucket** (Subscriptions, One-time Purchases, Orders, etc. each have independent 3,000/min buckets); more can be requested via Google Cloud Console if ever needed. |

- Source: https://developers.google.com/android-publisher/quotas

## 9. FusedLocationProviderClient / Android Geocoder — Phase 2

- No published rate limit for either — both are on-device Play Services APIs, not metered cloud calls from the app's perspective. Normal battery/location-request-priority tuning (Phase 2 already uses `PRIORITY_BALANCED_POWER_ACCURACY`) is a power concern, not a quota concern.

## Licensing risk summary (read alongside main plan §5)

| Service | Free-tier legal scope | Status for this (monetized) app |
|---|---|---|
| Open-Meteo | Non-commercial only | ❌ Needs paid commercial plan before launch |
| RainViewer | **Personal/educational only** (stricter wording) | ❌ Needs a licensing conversation or must be scoped out for non-US users at launch |
| NWS (`api.weather.gov`) | Free for any use, including commercial | ✅ No action needed |
| ML Kit Translation | Free, unlimited, any use | ✅ No action needed |
| Gemma 4 (Apache 2.0) | Open-weight, permissive | ✅ No action needed |
| Firebase (Spark) | Free within quotas listed above | ✅ No action needed at this app's scale |
| AdMob / Play Billing | Standard developer terms, no special commercial restriction beyond normal policy compliance | ✅ No action needed beyond standard policy compliance |

## Sources
- Open-Meteo pricing/limits (fetched live): https://open-meteo.com/en/pricing
- NWS API general FAQ (fetched live): https://weather-gov.github.io/api/general-faqs
- RainViewer API docs (fetched live): https://www.rainviewer.com/api.html
- ML Kit Translation Android guide: https://developers.google.com/ml-kit/language/translation/android
- Firebase pricing (fetched live): https://firebase.google.com/pricing
- AdMob API quotas (fetched live): https://developers.google.com/admob/api/quotas
- AdMob ad serving limits: https://support.google.com/admob/answer/9493252
- Google Play Developer API quotas (fetched live): https://developers.google.com/android-publisher/quotas
