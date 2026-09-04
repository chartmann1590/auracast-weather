import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.parcelize")
    // Firebase — google-services.json now present (real project: auracast-weather)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

// ── AdMob IDs — SECURE, NEVER hardcoded ─────────────────────────────────────
// Real production IDs are stored ONLY in:
//   • local.properties (gitignored, local dev)  → admob.appId / admob.bannerId / admob.interstitialId
//   • GitHub Actions Secrets (CI)               → ADMOB_APP_ID / ADMOB_BANNER_ID / ADMOB_INTERSTITIAL_ID
// Resolution order: env var (CI) → local.properties → Gradle property → fallback TEST ID.
// Debug builds ALWAYS use Google's public TEST IDs to avoid invalid-traffic policy strikes.
// Release builds use the real IDs when present (local or CI), otherwise fall back to test IDs
// so forks/PRs and fresh clones still build without secrets.
val _adMobLocalProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { ins -> load(ins) }
    }
}
fun _adMobSecret(propKey: String, envKey: String, fallback: String): String {
    val fromEnv = System.getenv(envKey)?.trim()?.takeIf { it.isNotEmpty() }
    val fromLocal = _adMobLocalProps.getProperty(propKey)?.trim()?.takeIf { it.isNotEmpty() }
    val fromProject = (findProperty(propKey) as? String)?.trim()?.takeIf { it.isNotEmpty() }
    // Explicitly handle nullable chain to satisfy Kotlin compiler
    if (fromEnv != null) return fromEnv
    if (fromLocal != null) return fromLocal
    if (fromProject != null) return fromProject
    return fallback
}
val _adMobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val _adMobTestBanner = "ca-app-pub-3940256099942544/9214589741"
val _adMobTestInterstitial = "ca-app-pub-3940256099942544/1033173712"
val _adMobRealAppId = _adMobSecret("admob.appId", "ADMOB_APP_ID", _adMobTestAppId)
val _adMobRealBanner = _adMobSecret("admob.bannerId", "ADMOB_BANNER_ID", _adMobTestBanner)
val _adMobRealInterstitial = _adMobSecret("admob.interstitialId", "ADMOB_INTERSTITIAL_ID", _adMobTestInterstitial)

// ── Signing — upload keystore for Play (local keystore.properties, CI via env + base64 decode in workflow)
val _keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun _keystoreSecret(propKey: String, envKey: String): String? {
    val fromEnv = System.getenv(envKey)?.trim()?.takeIf { it.isNotEmpty() }
    if (fromEnv != null) return fromEnv
    val fromProps = _keystoreProps.getProperty(propKey)?.trim()?.takeIf { it.isNotEmpty() }
    if (fromProps != null) return fromProps
    val fromProject = (findProperty(propKey) as? String)?.trim()?.takeIf { it.isNotEmpty() }
    return fromProject
}
val _storeFilePath = _keystoreSecret("storeFile", "KEYSTORE_FILE") ?: "upload-keystore.jks"
val _storePassword = _keystoreSecret("storePassword", "KEYSTORE_PASSWORD")
val _keyAlias = _keystoreSecret("keyAlias", "KEY_ALIAS") ?: "upload"
val _keyPassword = _keystoreSecret("keyPassword", "KEY_PASSWORD")

android {
    namespace = "com.auracast.weather"
    compileSdk = 36

    signingConfigs {
        create("release") {
            // Only configure if keystore file + passwords are present (local or CI). Otherwise unsigned allows forks/PRs to still build.
            val ksFile = rootProject.file(_storeFilePath)
            // In CI the workflow decodes KEYSTORE_BASE64 into upload-keystore.jks at root before this runs
            val ciFallback = rootProject.file("upload-keystore.jks")
            val resolvedFile = when {
                ksFile.exists() -> ksFile
                ciFallback.exists() -> ciFallback
                else -> null
            }
            if (resolvedFile != null && _storePassword != null && _keyPassword != null) {
                storeFile = resolvedFile
                storePassword = _storePassword
                keyAlias = _keyAlias
                keyPassword = _keyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = "com.auracast.weather"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // Safe default for manifest merger; per-buildType overrides below enforce test-vs-real policy.
        manifestPlaceholders["adMobAppId"] = _adMobTestAppId
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            // Debug ALWAYS uses TEST IDs — never real, even if secrets are present locally.
            manifestPlaceholders["adMobAppId"] = _adMobTestAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${_adMobTestBanner}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${_adMobTestInterstitial}\"")
            buildConfigField("boolean", "USE_TEST_ADS", "true")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release uses REAL IDs when secrets are present (local.properties or CI env), else test fallback so build never breaks.
            manifestPlaceholders["adMobAppId"] = _adMobRealAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${_adMobRealBanner}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${_adMobRealInterstitial}\"")
            buildConfigField("boolean", "USE_TEST_ADS", "false")
            // Sign with upload keystore when present; otherwise unsigned (still builds, just not Play-ready)
            val hasSigning = (rootProject.file(_storeFilePath).exists() || rootProject.file("upload-keystore.jks").exists()) && _storePassword != null && _keyPassword != null
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.58")
    ksp("com.google.dagger:hilt-compiler:2.58")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // Bumped from 1.8.1 — that pin caused a real crash: litertlm-android's Conversation
    // class calls a SendChannel.close$default overload that only exists in newer
    // kotlinx-coroutines (java.lang.NoSuchMethodError at runtime during report generation).
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // Room + DataStore
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Firebase — real project (auracast-weather), google-services.json in place
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // Ads & Billing (Phase 10) — gated behind isAdFree/BillingManager flag
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation("com.android.billingclient:billing-ktx:9.1.0")

    // ML Kit Translation (Phase 7) — on-device, downloadable language packs
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0") // Task<T>.await() bridge, used by ML Kit calls

    // Glance Widget (Phase 11)
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // Lottie (Phase 11/16 — Meteocons animated icons)
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // LiteRT-LM Android (Phase 5) — on-device Gemma 4 inference.
    // Source: https://developers.google.com/edge/litert-lm/android
    // Version pinned to the real latest release found on dl.google.com's maven-metadata.xml.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")

    // Google Fonts downloadable-fonts API (Phase 16) — Lexend/Manrope/Inter fetched at runtime,
    // cached by the OS font provider; no font files bundled in the APK.
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Coil3 + SVG decoder (Phase 16) — renders the real Meteocons SVGs bundled in
    // app/src/main/assets/meteocons/ (MIT license, see LICENSE.txt alongside them).
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-svg:3.4.0")
    // Network fetcher (Phase 4 radar tiles) — the app's ImageLoader only ever needed local
    // asset SVGs before this, so it had zero network-capable fetcher registered. Without this,
    // Coil throws "Unable to create a fetcher that supports: https://..." for any http(s) URL —
    // confirmed via on-device logging, this was the actual root cause of radar tiles never
    // loading, not an osmdroid bug.
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

    // osmdroid (Phase 4) — free OSM base map, no API key/billing (unlike Google Maps SDK).
    // Radar tiles (RainViewer/NWS) are layered on top via a custom TilesOverlay.
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
