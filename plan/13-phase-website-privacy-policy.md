# Phase 13 — Firebase-Hosted Website & Privacy Policy

**Goal:** a small marketing site + a legally sound privacy policy, both required before Play Store submission (Phase 12), hosted free on Firebase Hosting.

## Firebase Hosting setup

- Confirmed free tier: **10GB storage, 360MB/day data transfer**, automatic HTTPS on every deploy, custom domain support (free, just needs DNS records pointed at Firebase). Source: https://firebase.google.com/products/hosting
- Static site — no framework required for this scope; a simple static HTML/CSS build (or a lightweight generator like Astro/Eleventy if you want componentization) is enough for a one-app marketing page.

```bash
npm install -g firebase-tools
firebase login
firebase init hosting   # select the same Firebase project used for Crashlytics/Perf/FCM
firebase deploy --only hosting
```

- `firebase.json` should set long cache headers on static assets and a short/no-cache on `index.html` so updates propagate immediately:

```json
{
  "hosting": {
    "public": "public",
    "headers": [
      { "source": "**/*.@(js|css|png|jpg|svg)", "headers": [{ "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }] },
      { "source": "/index.html", "headers": [{ "key": "Cache-Control", "value": "no-cache" }] }
    ]
  }
}
```

## Site content

- **Home**: app name, tagline (the AI-podcast-weather hook), hero screenshot/GIF, Play Store badge/link (add once the listing is live), feature highlights matching the store listing copy from Phase 12.
- **Support**: an email or contact form for user support requests (Play Console requires a support contact; a Firebase-hosted `mailto:` link or a simple Firestore-backed contact form both satisfy this — a `mailto:` link is enough for v1, no backend needed).
- **Privacy Policy** (own page, linked from both the site footer and the Play Console listing).

## Privacy policy — must accurately reflect what the app actually does

Write this from the real data flows established in earlier phases, not boilerplate:

- **Location data**: coarse device location (Phase 2) used only to fetch weather/radar for the user's area; never sold, never used for ad targeting beyond standard AdMob SDK behavior (disclose AdMob's own data collection via its SDK, since it's a third party embedded in the app — link to Google's AdMob privacy disclosures).
- **Crash & performance data**: Firebase Crashlytics/Performance Monitoring (Phase 9) collects device model, OS version, stack traces, anonymous install ID — disclose this as diagnostic data, not tied to a real identity.
- **On-device AI (Gemma 4) and translation (ML Kit)**: explicitly state that weather-report generation and translation happen **on-device** and forecast content is **not sent to any AI service** — this is a genuine differentiator worth stating plainly and accurately (don't oversell it into a claim the architecture doesn't support — e.g. if the AICore path in Phase 5 involves any Google-side model management/telemetry, disclose that precisely rather than blanket-claiming "nothing ever leaves the device").
- **Advertising ID & AdMob**: disclose use of the Advertising ID, AdMob's data collection, and provide the required opt-out/consent mechanism (UMP SDK, Phase 10) for GDPR/CCPA-applicable users.
- **Purchases**: Play Billing transaction data is handled by Google Play, not stored by the app itself beyond entitlement state.
- **No account system**: state plainly that the app doesn't require sign-in and doesn't collect name/email/etc. (keeps this simple as long as Phase 12's "no login" decision holds).

Use a reputable privacy-policy generator only as a starting skeleton (e.g. the ones surfaced by TermsFeed/FreePrivacyPolicy/Termly), then **hand-edit every section to match the actual data flows above** — a generic generated policy that doesn't mention on-device AI, ML Kit, AdMob, and Play Billing specifically will fail Play Console's policy review scrutiny for accuracy.

## Acceptance criteria
- Site is reachable over HTTPS at the Firebase Hosting URL (and custom domain, if configured) before Play Console submission.
- Privacy policy page is a stable, permanent URL (not subject to being torn down) and accurately lists every data category in the checklist above.
- Play Console's Data Safety form (Phase 12) and this privacy policy are consistent with each other — cross-check line by line before submission.

## Sources
- Firebase Hosting free tier limits: https://firebase.google.com/products/hosting
- Privacy policy structure guidance for Firebase-based apps: https://www.termsfeed.com/blog/firebase-privacy-policy/ and https://termly.io/resources/articles/privacy-policy-for-firebase/
