# Phase 11 — Visual Polish: Icons, Animation, Dark Mode, Widgets

**Goal:** the "wow" layer — this is what makes the app feel premium and screenshot-worthy for the Play Store listing, not just functional.

> See `16-design-system-and-visual-assets.md` for the full color/type system, exact sourced+licensed icon/illustration assets (Meteocons, Material Symbols, unDraw/Storyset, Google Fonts), and a screen-by-screen flow spec. This file covers the *build tasks*; that file covers *what to build it with* and *how it should feel*.

## Icon set

- Build a custom animated weather-icon set (don't ship generic clipart) covering every WMO weather_code used in Phase 3, in day/night variants: clear, partly cloudy, overcast, fog, drizzle, rain, freezing rain, snow, thunderstorm, etc. (~25–30 icons × 2 = ~50 assets).
- Format: **Lottie** (`.json`, via `com.airbnb.android:lottie-compose`) for anything with looping motion (rain falling, sun rays pulsing, clouds drifting, lightning flashing) — vector-based, small file size, GPU-cheap. Static fallback `ImageVector`/SVG for the app launcher icon and notification small-icon (Lottie doesn't work in those contexts).
- Source options: commission original Lottie animations, or adapt from a licensed animated-icon set (LottieFiles has both free-with-attribution and commercial-license packs — pick a commercial-license pack if the app is monetized, to stay clean on licensing given the ad/subscription revenue model).

## Motion design

- **Home screen background**: `Canvas`/`Brush` gradient that subtly animates (slow hue/position drift) keyed to condition + time-of-day, using `rememberInfiniteTransition`.
- **Screen transitions**: shared-element transitions (Compose `SharedTransitionLayout`, stable since recent Compose releases) between the Home hero card and the Report tab's "album art," reinforcing the podcast metaphor.
- **Micro-interactions**: pull-to-refresh with a custom weather-icon spinner instead of the default Material spinner; hourly-strip items scale/elevate slightly on scroll-snap.
- **Loading states**: skeleton loaders (shimmer) for each card independently rather than one full-screen spinner — reinforces the "cache-then-network" architecture from Phase 3 by showing structure immediately.
- Keep all animation on the Compose animation APIs (`animateFloatAsState`, `Animatable`, `InfiniteTransition`) — avoid heavyweight custom rendering loops that fight Compose's recomposition model.

## Dark mode

- Full Material 3 dynamic color (`dynamicDarkColorScheme`/`dynamicLightColorScheme` on Android 12+, static branded fallback palette below that) — respects system theme by default with an in-app override (Light/Dark/System) in Settings.
- Weather gradient backgrounds need distinct dark-mode variants, not just a dimmed version of the light palette (e.g. night-clear should read as deep indigo with visible stars, not a muddy gray).

## Home screen widget

- A Glance-based (`androidx.glance:glance-appwidget`) widget showing current temp + icon + condition, tap-to-open, refreshed via the same `WeatherRepository` cache — reuses Phase 3 data layer entirely, no new backend logic.

## App icon & branding

- Adaptive icon (`ic_launcher.xml` foreground/background layers) reflecting the brand mark, tested against all major launcher mask shapes (circle, squircle, rounded square) via Android Studio's adaptive icon preview.

## Acceptance criteria
- Every WMO weather code used by the app resolves to a real, non-placeholder icon in both day and night variants — verified by a debug screen that renders the full icon grid.
- App looks visually distinct in a side-by-side with 3 competitor weather apps (subjective but explicitly checked before Phase 12 store listing screenshots are taken).
- Dark mode has zero low-contrast/unreadable text (checked with Android Studio's accessibility scanner).

## Sources
No live external claims made in this phase beyond already-established Compose APIs (Phase 1/3 sources) and standard `androidx.glance` widget APIs. Icon/illustration/font vendor choices and their licenses are researched and fixed in `16-design-system-and-visual-assets.md` — use that file's asset list rather than sourcing icons ad hoc during implementation.
