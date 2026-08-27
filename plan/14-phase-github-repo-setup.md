# Phase 14 — Private GitHub Repo & Source Control

**Goal:** the codebase lives in a private repo with a sane branch strategy and secrets kept out of git history from commit #1.

## Repo creation

```bash
gh repo create <your-username>/auracast-weather --private --description "AI-narrated on-device weather app" --gitignore Kotlin
```

- Confirm it's private (Settings → General → Danger Zone shows visibility) — do this immediately, not after the first push, since a repo made public even briefly can leak history to forks/caches.

## `.gitignore` essentials (Android + this project's specifics)

```gitignore
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
*.keystore
*.jks
/app/google-services.json.bak

# Model weights if ever staged locally for LiteRT-LM testing (Phase 5) — never commit multi-GB model files
*.litertlm
*.task
/app/src/main/assets/models/
```

- `google-services.json` (Firebase) **is safe to commit** — it's a client config file, not a secret, per Google's own guidance — but the release **keystore** (`*.jks`/`*.keystore`) and any `local.properties`/API-adjacent secrets are never committed.
- Gemma 4 model weight files (if using the embedded LiteRT-LM path from Phase 5 during local testing) are multi-GB — never belong in git; they're distributed via Play Asset Delivery at build/release time, and pulled from Hugging Face for local dev, not stored in-repo.

## Secrets handling

- Release signing keystore + its passwords: store outside the repo entirely (password manager / GitHub Actions encrypted secrets for CI signing), reference via `local.properties`-style `keystore.properties` that's itself gitignored, loaded in `build.gradle.kts`:

```kotlin
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(FileInputStream(f))
}
```

- CI secrets (if release signing/deployment is automated later): GitHub Actions **Repository secrets**, never hardcoded in workflow YAML.

## Branch strategy

- `main` — always releasable, protected (require PR review + green CI from Phase 1's workflow before merge).
- Short-lived feature branches per phase/task (`feat/phase-3-weather-core`, `fix/radar-zoom-clamp`), squash-merged to keep `main` history readable phase-by-phase, mirroring this plan's structure.
- Tag releases matching Play Console version codes (`v0.1.0`, `v1.0.0`) for traceability between a Play Store build and the exact source snapshot that produced it.

## Repo layout at root

```
/plan/                  this planning folder
/app/                   Android app module
/website/               Firebase Hosting site (Phase 13) — or a separate repo, see note below
/.github/workflows/     CI (Phase 1)
firebase.json
keystore.properties     (gitignored, never committed)
README.md
```

**Note on the website**: keep it in the same repo under `/website/` for a solo/small-team project (simplest — one place, one history); split it into its own repo only if the site gains its own contributors or release cadence independent of the app. Don't split prematurely.

## Acceptance criteria
- `git log` on `main` shows no keystore files, no `.litertlm`/model weight blobs, no plaintext secrets, ever (spot-check with `git log --all --full-history -- '*.jks'` returning nothing).
- Branch protection on `main` is active and verified by attempting a direct push (should be rejected).
- A fresh `git clone` + the setup steps from Phase 1 produces a buildable project with only `google-services.json` and `keystore.properties` needing to be supplied out-of-band.

## Sources
No live external claims in this phase — standard, well-established Git/GitHub repo hygiene practices; `google-services.json` non-secret status is standard, widely documented Firebase/Google guidance rather than a fact requiring live verification for this plan.
