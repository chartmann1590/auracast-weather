// Top-level build file — AuraCast Weather
// Plan reference: plan/01-phase-foundation-setup.md

plugins {
    // NOTE: Compose BOM 2026.08.00 (Compose UI 1.12.0) requires AGP 9.1.0+ / compileSdk 37,
    // confirmed via a real `./gradlew compileDebugKotlin` run. AGP 9.x is itself a breaking
    // major-version migration (Gradle 9.x required, namespace/API removals) that's out of
    // scope for this pass — pinned Compose to the newest BOM that still builds cleanly on
    // AGP 8.13.2 / compileSdk 36 instead (bumped from 8.5.2, which the prior Compose BOM
    // 2026.06.01 also failed against — needs 8.6.0+; 8.13.2 is the latest 8.x). Gradle
    // wrapper bumped to 8.14.5 to match (AGP 8.13 requires Gradle 8.13+).
    id("com.android.application") version "8.13.2" apply false
    // Kotlin/KSP bumped from 2.0.21 — new deps added during this review pass (Coil3, LiteRT-LM,
    // Billing 9.1) ship metadata compiled with Kotlin 2.2+, which a 2.0.21 compiler can't read
    // ("Module was compiled with an incompatible version of Kotlin"). Landed on 2.3.21 rather
    // than the newer 2.4.10 because Hilt 2.58's javac processor caps at Kotlin metadata 2.3.0
    // ("Provided Metadata instance has version 2.4.0, while maximum supported version is
    // 2.3.0") — both constraints confirmed via real build failures. KSP 2.3.x is
    // version-independent of the Kotlin compiler (KSP2), so it pairs fine with 2.3.21.
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    // Hilt pinned to 2.58, not the newer 2.60 — confirmed via a real build failure that
    // Hilt 2.59+ dropped AGP 8.x support outright ("only compatible with AGP 9.0.0+").
    // 2.58 is the last release that still works with our AGP 8.13.2 pin.
    id("com.google.dagger.hilt.android") version "2.58" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.3" apply false
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.3.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
