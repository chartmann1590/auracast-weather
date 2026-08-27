# Phase 16 — Design System & Visual Asset Sourcing

**Goal:** make sure "beautiful, flows nicely, doesn't look like shit" isn't a vibe — it's a concrete, sourced, licensed asset list plus a real interaction spec, so Phase 11 (Polish) has nothing left to guess at.

This phase supplements Phase 11. Phase 11 says *what* to build (icons, motion, dark mode, widgets); this phase says **exactly which real, licensed assets to pull in, and precisely how each screen is laid out and transitions to the next**, so the app has one coherent visual system instead of a grab-bag of default Material widgets.

---

## 1. Visual identity

### Color system
- Build on **Material 3 dynamic color** (Phase 11) as the *structural* system (surfaces, text contrast, elevation), but layer a **custom weather-condition palette** on top for anything condition-driven — dynamic color alone will make every weather app look like every other Android app; the condition palette is what makes this one feel like a weather app specifically.

| Condition | Light accent | Dark accent | Mood |
|---|---|---|---|
| Clear day | `#4FA8FF` → `#FFD873` gradient | `#0B1E3D` → `#3A2E00` | bright, optimistic |
| Clear night | `#0B1E3D` → `#2B0B5E` | same (already dark-native) | starry indigo/violet |
| Partly cloudy | `#7FB8E8` → `#C9D6E3` | `#1C2B3A` → `#33465A` | soft, neutral |
| Overcast | `#8A97A6` → `#B8C2CC` | `#242A31` → `#3A4148` | muted gray-blue |
| Rain | `#4A6FA5` → `#8FA9C9` | `#141D2B` → `#26374F` | cool, saturated blue |
| Thunderstorm | `#3A3A5E` → `#6E4B8A` | `#0E0E1C` → `#2A1A3B` | dramatic violet-slate |
| Snow | `#DCE9F5` → `#B9CEE0` | `#1B2733` → `#33455A` | crisp, desaturated |
| Fog | `#B6BEC4` → `#D6DBDE` | `#23272B` → `#3A3F44` | flat, low-contrast on purpose |

- Derive these into Compose `Brush.verticalGradient` backgrounds for the Home hero (Phase 11) — build them as a small `WeatherPalette` lookup keyed by WMO code + is-night, not hardcoded per-screen.

### Typography
- **Google Fonts, free, OFL-licensed** (no cost, no attribution required, safe for commercial apps): pair a rounded/friendly display face for big numbers with a clean workhorse for body text.
  - Display/hero numbers (the big temperature): **Lexend** or **Manrope** — both free on Google Fonts, geometric/modern, read well at huge sizes.
  - Body/UI text: **Inter** — the de facto standard for clean UI text, free, huge language coverage (helps the ML Kit translation phase, since Inter has broad glyph support).
  - Pull both via `androidx.compose.ui.text.googlefonts` (the Compose "Downloadable Fonts" API) rather than bundling font files, so they're fetched from Google's font provider at runtime and cached by the OS — smaller APK, always up to date.
  - Sources: https://fonts.google.com/specimen/Lexend · https://fonts.google.com/specimen/Manrope · https://fonts.google.com/specimen/Inter

### Iconography (UI chrome — nav bar, settings, buttons — NOT weather condition icons)
- **Material Symbols** (Google's own icon system) — Apache 2.0 licensed, free for any use including commercial, variable-font with fill/weight/grade/optical-size axes so icons can visually "breathe" (e.g. a filled bell icon when notifications are on, outlined when off, animated between the two states).
- Available directly as an `androidx.compose.material.icons.extended` dependency (a large chunk is already bundled with Compose) or pulled individually from the Material Symbols library for anything not in the extended set.
- Source: https://github.com/google/material-design-icons (Apache 2.0) · https://developers.google.com/fonts/docs/material_symbols

---

## 2. Weather condition icons (the single most important visual asset in the app)

**Use Meteocons.** This is a real, actively maintained, purpose-built icon set that solves exactly this problem:

- **What it is**: 500+ hand-crafted, *animated* weather icons (sun rays pulsing, clouds drifting, rain falling, lightning flashing) by developer Bas Milius, designed in Figma, shipped as both animated **SVG** and **Lottie JSON**.
- **Styles**: fill, flat, line, and monochrome — pick one consistent style app-wide (recommend **fill** for the Home hero icon at large size, **line** or **monochrome** for small inline icons like the hourly strip, to avoid visual noise at small sizes).
- **Coverage**: weather conditions (clear/cloudy/rain/snow/storm/fog/etc., day & night variants), thermometers, barometers, wind indicators, moon phases, UV index, alert glyphs — covers every WMO code this app needs (Phase 3) plus extras (moon phase, UV) that upgrade the daily-forecast cards beyond a generic competitor.
- **License**: **MIT** — free for commercial use, no attribution required, modification allowed.
- **Integration**: `npm install @meteocons/svg` or `@meteocons/lottie`, or pull directly from the GitHub repo; render Lottie variants via `com.airbnb.android:lottie-compose` (already planned in Phase 11) for the Home hero and Report-tab "album art," and static SVG (via Compose's SVG/vector support, or pre-rasterized to `ImageVector`) for small icons like the hourly strip and 5-day list where animation would be distracting.
- Sources: https://meteocons.com/ · https://github.com/basmilius/meteocons · license: MIT (stated on the GitHub repo)

**Do not** source weather icons from a random Flaticon/Freepik pack instead — Meteocons is purpose-built, animated, and free-and-clear, which is strictly better for both quality and licensing simplicity than piecing together static icons from a general stock site.

---

## 3. Illustrations (onboarding, empty states, permission-request screens)

Two free, commercially-safe sources, used for different jobs:

- **unDraw** (https://undraw.co) — flat, single-accent-color illustrations, recolor to match the app's palette on their site before export. **License**: free for commercial and non-commercial use, no attribution required (confirmed on undraw.co/license). Best for **empty states**: "no saved locations yet," "location permission denied," "no internet connection."
- **Storyset** (https://storyset.com, from the Freepik team) — more expressive, animatable character illustrations in multiple styles (Rafiki, Bro, Amico, Pana), can toggle simple built-in animations and recolor before export. **License**: free with required attribution (a visible credit link to Storyset) unless upgraded to a paid Freepik plan. Best for the **onboarding flow** (3–4 screens introducing location access, the AI podcast feature, and translation) where a bit more personality/animation earns its place.
- **Action item**: if avoiding any attribution requirement app-wide is a hard preference, use unDraw for onboarding too instead of Storyset — note this as an open decision for whoever executes Phase 11/16, not pre-decided here since it's a stylistic trade-off (Storyset's illustrations are more distinctive; unDraw is zero-friction legally).

---

## 4. Motion asset sourcing (beyond what Meteocons already covers)

- **Splash screen**: use Android 12+'s native `SplashScreen` API (system-managed, not a custom asset-heavy splash) with the app's icon + a single brand color — avoids the dated "custom splash Activity with a full-screen logo" anti-pattern and is the current platform-recommended approach.
- **Loading/skeleton shimmer** (Phase 3's per-card skeleton loaders): build with Compose's own `Brush.linearGradient` + `animateFloat` — no external asset needed, this is pure code and looks better tightly matched to each card's exact shape than a generic imported shimmer asset would.
- **Pull-to-refresh spinner**: swap Material's default spinner for a small Meteocons Lottie loop (e.g. a spinning cloud or sun) — reuses the already-licensed icon set instead of sourcing a separate spinner asset.
- **Confetti/celebration moment** (optional, e.g. first successful AI report generated) — if desired, `LottieFiles` public marketplace animations are covered by the **Lottie Simple License**, which permits commercial use with no attribution required; pick a specific, named animation at implementation time and record its marketplace URL in a code comment for provenance. Source: https://lottiefiles.com/page/license

---

## 5. Screen-by-screen flow (how it actually feels to use)

This is the concrete interaction spec so "flows nicely" is buildable, not just aspirational.

### 5.1 First launch
1. **Splash** (native SplashScreen API, <1s) →
2. **Onboarding** (3 Storyset/unDraw-illustrated screens, swipeable, skippable): (a) "Know before you go" — location access pitch with the permission prompt triggered from a button *on this screen*, not silently on app start (higher grant rates, and lets the user read the "why" first); (b) "Your weather, narrated" — a 5-second looping preview of the AI podcast waveform/icon, teasing Phase 5/6; (c) "In your language" — a quick language picker that pre-warms the Phase 7 ML Kit flow if a non-English language is chosen.
3. Lands on **Home** with either the resolved GPS location or a "search for your city" prompt if location was denied — never a dead end.

### 5.2 Home screen
- **Hero card** (top ~40% of screen): condition-gradient background (§1), large animated Meteocons icon, big temperature in Lexend/Manrope, condition text + high/low in Inter, all over the dynamic gradient. This card is the single most important pixel-area in the app — it should look complete and considered even with zero scrolling.
- **Hourly strip** immediately below: `LazyRow`, snap-to-item, each card = time, small monochrome-style Meteocons icon, temp, thin precip-probability bar. Scale/elevate slightly on scroll-snap (Phase 11).
- **5-day list** below that: each row's icon uses the *fill*-style Meteocons variant at small size, plus a horizontal min–max temperature bar visualized against the week's overall range (not just that day's range) so a glance at the whole list shows relative "which day is hottest" at a glance — a small detail that reads as considered rather than generic.
- **AI Report entry point**: a distinct, elevated card (not just a nav-bar tab) teasing the day's headline from the report ("Sunny with a late storm — full report →") that deep-links into the Report tab. Surfacing it *on* Home, not only behind a tab, is what makes the AI feature feel central rather than buried.
- **Banner ad** (Phase 10) sits at the very bottom, below all content, in its own fixed-height container — never interleaved between forecast cards (interstitial-style mid-content ad placement reads as "cheap" and hurts both UX and long-term ad performance).

### 5.3 Report ("podcast") screen
- Framed exactly like a music/podcast player (Phase 6): big animated icon in place of album art, karaoke-synced script text, transport controls, a scrub bar. Shared-element transition from the Home teaser card's icon into this screen's "album art" position (Compose `SharedTransitionLayout`) so it feels like one continuous motion, not a hard cut.
- While generating (Phase 5 inference in progress): show the album-art icon already in place, animating gently (idle pulse), with the script area showing a shimmer skeleton that gets replaced sentence-by-sentence as tokens stream in — never a blank screen with only a spinner.

### 5.4 Radar screen
- Map fills the screen edge-to-edge; play/pause + scrub bar float in a translucent bottom sheet over the map (not a separate panel competing for space); location pin marker uses a small pulsing dot (Compose `InfiniteTransition`) rather than a static pin, reinforcing "live" data.
- Attribution text (NWS/RainViewer, per Phase 15's licensing requirements) sits small but legible in a bottom corner, styled to not fight the map for attention but never hidden/obscured (policy requirement, not just a style preference).

### 5.5 Settings
- Grouped list (Units / Notifications / Language / Appearance / Subscription / About), Material 3 list-item styling, each destructive/high-consequence action (delete a saved location, clear a downloaded translation model) behind a confirm step.
- Subscription row shows current state clearly ("Free — ads shown" vs. "AuraCast Plus — ad-free") with a single tappable upgrade/manage CTA, not buried in a submenu.

### 5.6 Empty/error states (every one of these gets a real unDraw illustration, not a bare text string)
- No location permission + no manual location set yet.
- Network unreachable with no cache available (first-ever launch offline — rare but must not be a blank white screen).
- ML Kit language model download failed.
- AI report engine unavailable on this device (Phase 5's template fallback still applies here — this state is "less flavorful," not "broken").

---

## 6. Consolidated asset source list (for quick reference during implementation)

| Asset type | Source | License | Cost |
|---|---|---|---|
| Weather condition icons (animated) | Meteocons — https://meteocons.com / https://github.com/basmilius/meteocons | MIT | Free |
| UI chrome icons | Material Symbols — https://github.com/google/material-design-icons | Apache 2.0 | Free |
| Display font | Lexend / Manrope — https://fonts.google.com | OFL | Free |
| Body font | Inter — https://fonts.google.com/specimen/Inter | OFL | Free |
| Onboarding/empty-state illustrations | unDraw — https://undraw.co | Free, no attribution | Free |
| Onboarding illustrations (alt., more animated) | Storyset — https://storyset.com | Free, attribution required | Free |
| Optional celebratory animation | LottieFiles marketplace — https://lottiefiles.com | Lottie Simple License (commercial OK, no attribution) | Free |
| Splash screen | Android native `SplashScreen` API | N/A (platform API) | Free |

## Sources
- Meteocons (icons, license, formats): https://meteocons.com/ · https://github.com/basmilius/meteocons
- Material Symbols (license, coverage): https://github.com/google/material-design-icons · https://developers.google.com/fonts/docs/material_symbols
- unDraw license: https://undraw.co/license
- Storyset (attribution requirement): https://pixels.market/blog/storyset-review-alternatives
- LottieFiles Simple License: https://lottiefiles.com/page/license
- Google Fonts (Lexend, Manrope, Inter — OFL licensing is Google Fonts' standard license for all hosted families): https://fonts.google.com
