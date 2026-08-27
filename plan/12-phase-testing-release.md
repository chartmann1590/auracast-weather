# Phase 12 — Testing Strategy & Play Store Release

**Goal:** ship confidently, with a rollout plan that catches problems before 100% of users see them.

## Test strategy

- **Unit tests** (JUnit): repository logic (cache-then-network fallback chain from Phase 3), unit-conversion math, prompt-template construction (Phase 5), notification dedupe logic (Phase 8).
- **Instrumented/UI tests** (Compose UI testing + Espresso where needed): critical flows — location permission grant/deny paths, manual location search, generating and playing an AI report end-to-end (mock the LLM engine with a fake `AiReportEngine` for determinism in CI), purchase flow (Play Billing has a documented test-purchase mode).
- **Device matrix** (critical given the Gemma 4 RAM-tiering in Phase 5): test on at least one low-RAM (<3GB) device/emulator, one E2B-tier (3–6GB), one E4B-tier (6GB+) device, and both an AICore-eligible and a non-eligible Android version, to exercise every branch of `AiReportEngineSelector`.
- **Manual QA pass** covering every phase's acceptance criteria section as a literal checklist before each release.

## Pre-launch checklist (Play Console)

- **Data safety form**: declare precisely what's collected — location (coarse), crash/perf telemetry (Firebase), advertising ID (AdMob), no account/PII collection if the app stays anonymous (recommended for v1 — no login system needed for any feature in this plan).
- **Content rating questionnaire**, **target audience** (not directed at children — simplifies ad/COPPA compliance considerably; keep it that way unless there's a strong reason not to).
- **Privacy policy URL** → points to the Firebase-hosted site from Phase 13, must be live *before* the Play Console listing can be submitted for review.
- **Store listing**: screenshots (phone + optional tablet), a short + full description, feature graphic, app icon (from Phase 11). Highlight the two differentiators up top: "AI-narrated weather podcast, fully on-device" and "Live radar, no account needed."

## Rollout

1. **Internal testing track** (Firebase App Distribution or Play Console internal track) — you + a few testers, for at least a week covering the device matrix above.
2. **Closed testing track** — small external group, focus on real-world location diversity (rural GPS accuracy, international users to stress Open-Meteo/RainViewer fallback paths) and real Play Billing purchases.
3. **Open testing / staged production rollout** — Play Console supports percentage-based staged rollout (e.g. 10% → 25% → 50% → 100%); watch the Crashlytics velocity alert (Phase 9) at each stage before advancing, halt/rollback if crash-free-users% drops meaningfully.

## Release build hygiene

- Enable R8 full-mode + resource shrinking for release builds; keep a maintained `proguard-rules.pro` with explicit `-keep` rules for Retrofit/kotlinx.serialization DTOs, Room entities, and any reflection-based Firebase/AdMob/Billing classes that need it (consult each SDK's own consumer ProGuard rules — most ship their own and need no manual keep rules, verify via a release-build smoke test rather than assuming).
- Signing: use Play App Signing (upload key + Google-managed app signing key), store the upload keystore outside the repo (see Phase 14 secrets handling).

## Acceptance criteria
- CI runs the full unit + instrumented suite green on every PR before merge to `main`.
- A release-config build installs, launches, and passes the full manual QA checklist on all four device-matrix tiers.
- Staged rollout reaches 100% with no Crashlytics velocity alert triggered.

## Sources
No new external claims beyond already-cited Play Console/Billing/Firebase sources from earlier phases; release-process steps reflect standard, well-established Play Console mechanics (staged rollout, data safety form, testing tracks) rather than a fact requiring live verification.
