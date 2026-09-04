# Fastlane — Play Store upload for AuraCast Weather

**Service account:** `play-publisher@auracast-weather.iam.gserviceaccount.com`
- Created in GCP project `auracast-weather` (`gcloud iam service-accounts create play-publisher`)
- `play-service-account.json` (2370 bytes) is **gitignored** — stored locally at `fastlane/play-service-account.json` for local runs, and as GitHub Secret `PLAY_SERVICE_ACCOUNT_JSON` for CI.
- **You must add the email to Play Console:** Play Console → Users and permissions → Invite new user → paste `play-publisher@auracast-weather.iam.gserviceaccount.com` → grant **Admin** (or Release Manager + View app info) → Invite. Wait for Google email confirmation.

**Promo video YouTube:** `https://youtu.be/33B6BICsm7c` (26.8s, 1920×1080, Zira voice, ends on github.com/chartmann1590/auracast-weather) — URL in `fastlane/metadata/android/en-US/video.txt` and website `website/public/index.html`.

**Metadata:** `fastlane/metadata/android/en-US/` contains `title.txt` (16 chars), `short_description.txt` (80 chars), `full_description.txt` (now with GitHub link), `video.txt`, `changelogs/1.txt`, `images/featureGraphic.png` (1024×500), `images/promo.mp4` + `promoPoster.png`, `phoneScreenshots/` (1080×2400), `sevenInchScreenshots/` (1200×1920), `tenInchScreenshots/` (1600×2560) — all committed.

**Local usage:**
```bash
# 1. Drop service account JSON at fastlane/play-service-account.json (gitignored)
# 2. Ensure upload-keystore.jks + keystore.properties (gitignored) present at repo root
./gradlew bundleRelease          # builds signed AAB when keystore present
bundle exec fastlane metadata_only  # uploads listing only
bundle exec fastlane beta        # builds AAB + uploads to internal track
bundle exec fastlane production  # promotes to production at 20%
```

**CI:** `.github/workflows/play-publish.yml` — on `workflow_dispatch` or push to `master` when tagged, decodes `PLAY_SERVICE_ACCOUNT_JSON` and `KEYSTORE_BASE64` secrets, builds signed `bundleRelease`, runs `fastlane beta`.

**GitHub Secrets required (Settings → Secrets → Actions):**
- `PLAY_SERVICE_ACCOUNT_JSON` — raw JSON content of `play-service-account.json` (2370 bytes, `type: service_account`). **Not base64** — paste file content verbatim.
- `KEYSTORE_BASE64` — base64 of `upload-keystore.jks` (3012 chars) — generate via `certutil -encode` or `base64 -w 0 upload-keystore.jks`
- `KEYSTORE_PASSWORD`, `KEY_ALIAS` (=`upload`), `KEY_PASSWORD` — from `keystore.properties`
- `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID` — already documented in `.github/workflows/build.yml` (same secrets).

**Signing:** `app/build.gradle.kts` reads `keystore.properties` (local) or env `KEYSTORE_PASSWORD` etc + file `upload-keystore.jks` / `KEYSTORE_BASE64` decode in CI. Debug builds always use test AdMob IDs; release uses real when secrets present.

**First publish checklist:**
1. Add service account email to Play Console and accept.
2. Create app `com.auracast.weather` manually in Play Console (once) — upload first AAB via UI to create listing, then `fastlane supply` can update.
3. Set `PLAY_SERVICE_ACCOUNT_JSON` etc in GitHub Secrets.
4. Run workflow `play-publish` → check Play Console Internal testing track.
5. Complete Data Safety (from `website/public/privacy.html`), Content Rating, App Content, set countries, pricing (free with ads, Plus via Play Billing).
6. Promote Internal → Closed → Production.
