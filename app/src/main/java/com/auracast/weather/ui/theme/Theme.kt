package com.auracast.weather.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontRef
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.auracast.weather.R

// Phase 16 §1 — typography. Fetched at runtime via the Downloadable Fonts API (cached by
// the OS font provider, no font files bundled in the APK). Falls back to the system's
// default sans-serif automatically if the Google Play Services font provider is unavailable
// (e.g. some China-market devices without GMS) — Compose handles that fallback internally.
@OptIn(ExperimentalTextApi::class)
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

@OptIn(ExperimentalTextApi::class)
private fun downloadableFontFamily(name: String, weights: List<FontWeight>): FontFamily =
    FontFamily(weights.map { weight -> GoogleFontRef(GoogleFont(name), fontProvider, weight = weight) })

val DisplayFontFamily = downloadableFontFamily("Lexend", listOf(FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold))
val BodyFontFamily = downloadableFontFamily("Inter", listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold))

val AuraCastTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
        displayMedium = base.displayMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = BodyFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = BodyFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = BodyFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontFamily = BodyFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = BodyFontFamily),
    )
}

// ConditionPalette — Phase 16 §1 — lookup keyed by WMO code + isNight
// Light / dark accent pairs; used as vertical gradients on Home hero.

data class ConditionGradient(val lightStart: Color, val lightEnd: Color, val darkStart: Color, val darkEnd: Color)

object WeatherPalette {
    val clearDay = ConditionGradient(Color(0xFF4FA8FF), Color(0xFFFFD873), Color(0xFF0B1E3D), Color(0xFF3A2E00))
    val clearNight = ConditionGradient(Color(0xFF0B1E3D), Color(0xFF2B0B5E), Color(0xFF0B1E3D), Color(0xFF2B0B5E))
    val partlyCloudy = ConditionGradient(Color(0xFF7FB8E8), Color(0xFFC9D6E3), Color(0xFF1C2B3A), Color(0xFF33465A))
    val overcast = ConditionGradient(Color(0xFF8A97A6), Color(0xFFB8C2CC), Color(0xFF242A31), Color(0xFF3A4148))
    val rain = ConditionGradient(Color(0xFF4A6FA5), Color(0xFF8FA9C9), Color(0xFF141D2B), Color(0xFF26374F))
    val thunderstorm = ConditionGradient(Color(0xFF3A3A5E), Color(0xFF6E4B8A), Color(0xFF0E0E1C), Color(0xFF2A1A3B))
    val snow = ConditionGradient(Color(0xFFDCE9F5), Color(0xFFB9CEE0), Color(0xFF1B2733), Color(0xFF33455A))
    val fog = ConditionGradient(Color(0xFFB6BEC4), Color(0xFFD6DBDE), Color(0xFF23272B), Color(0xFF3A3F44))

    fun forCode(wmoCode: Int, isNight: Boolean): ConditionGradient = when (wmoCode) {
        0, 1 -> if (isNight) clearNight else clearDay
        2, 3 -> partlyCloudy
        45, 48 -> fog
        51, 53, 55, 56, 57 -> rain // drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> rain
        71, 73, 75, 77, 85, 86 -> snow
        95, 96, 99 -> thunderstorm
        else -> overcast
    }
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4FA8FF),
    secondary = Color(0xFF8FA9C9),
    tertiary = Color(0xFFFFD873),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7FB8E8),
    secondary = Color(0xFF4A6FA5),
    tertiary = Color(0xFFC9A227),
)

@Composable
fun AuraCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = AuraCastTypography, content = content)
}
