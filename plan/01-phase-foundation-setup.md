# Phase 1 — Foundation & Project Setup

**Goal:** an empty-but-running Android app with the full dependency graph wired up, so every later phase is additive.

## Tasks

1. **Create the Android Studio project**
   - Empty Activity (Compose), package `com.auracast.weather` (rename to your domain).
   - Min SDK 26 (Android 8.0) — required floor for `WorkManager` reliability and ML Kit; Max/target SDK = latest stable (35/36 depending on Play Console requirement at build time — check Play Console "target API level requirements" page before submitting, it moves every year).
   - Kotlin 2.2, Compose compiler now bundled with the Kotlin Gradle plugin (no separate `composeOptions.kotlinCompilerExtensionVersion` needed) — confirmed by the Compose August '26 release notes.

2. **`app/build.gradle.kts` core dependencies**

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")       // Firebase
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
    id("kotlin-parcelize")
}

android {
    namespace = "com.auracast.weather"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.auracast.weather"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // DI
    implementation("com.google.dagger:hilt-android:2.56")
    ksp("com.google.dagger:hilt-compiler:2.56")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Local cache
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Background work
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Firebase (BoM keeps versions aligned)
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Ads & Billing — versions pinned in Phase 10
    // ML Kit Translation — pinned in Phase 7
    // LiteRT-LM — pinned in Phase 5

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

3. **Project-level Firebase setup**
   - Create a Firebase project in the console, register the Android app with `applicationId`, download `google-services.json` into `app/`.
   - Add `com.google.gms:google-services`, `com.google.firebase:firebase-crashlytics-gradle`, `com.google.firebase:perf-plugin` to the root `build.gradle.kts` classpath.
   - **Do not commit `google-services.json` API keys as secret** — it's safe to commit per Google's own guidance (it's not a security credential, it's a client identifier), but keep `keystore` and `local.properties` out of git regardless (see Phase 14 `.gitignore`).

4. **Module structure** — start single-module (`app`) for velocity; split into `:core:network`, `:core:database`, `:feature:home`, etc. only if build times become a problem (YAGNI — don't pre-split).

5. **Hilt application class**

```kotlin
@HiltAndroidApp
class AuraCastApp : Application()
```

Register in `AndroidManifest.xml` `android:name=".AuraCastApp"`.

6. **Base navigation scaffold** — `NavHost` with routes: `home`, `radar`, `report`, `settings`. Each is a placeholder Composable (`Text("TODO")`) until its phase lands.

7. **CI skeleton** (GitHub Actions, since repo is on GitHub — see Phase 14 for repo creation itself):

```yaml
# .github/workflows/build.yml
name: Build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }
      - run: ./gradlew assembleDebug lint testDebugUnitTest
```

## Acceptance criteria
- `./gradlew assembleDebug` succeeds.
- App launches to an empty Compose scaffold with bottom nav (Home / Radar / Report / Settings) and no crashes.
- Firebase console shows the app registered (real-time "app installed" ping visible under Crashlytics setup checklist).
- CI workflow runs green on first push.

## Sources
- Compose BOM / Kotlin compiler integration: https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html
- Firebase BoM & setup: https://firebase.google.com/docs/crashlytics/get-started
