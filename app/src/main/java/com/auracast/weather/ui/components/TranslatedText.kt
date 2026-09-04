package com.auracast.weather.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.hilt.navigation.compose.hiltViewModel
import com.auracast.weather.ui.screens.TranslationViewModel

/**
 * App-wide on-device translation helpers.
 *
 * [rememberTranslated] runs every visible string through the free ML Kit on-device pack for the
 * user's native language (chosen in onboarding, changeable in Settings). Once the pack is downloaded
 * it works fully offline. If the target is English or the model isn't ready yet, the original
 * English text is shown — never blank.
 *
 * Uses memory caching for zero-flicker instant rendering on recomposition and screen transitions.
 */
@Composable
fun rememberTranslated(original: String): String {
    if (original.isBlank()) return original
    val vm: TranslationViewModel = hiltViewModel()
    val lang by vm.currentLanguage.collectAsState()
    val revision by vm.revision.collectAsState()

    if (lang == "en") return original

    // Zero-flicker cached initialization
    val initialValue = vm.getCachedTranslation(original, lang) ?: original
    var translated by remember(original, lang, revision) { mutableStateOf(initialValue) }

    LaunchedEffect(original, lang, revision) {
        val result = vm.translate(original)
        if (result != translated) {
            translated = result
        }
    }
    return translated
}

/**
 * Helper to translate a template format string and substitute arguments.
 */
@Composable
fun rememberTranslatedFormat(formatTemplate: String, vararg args: Any): String {
    val translatedTemplate = rememberTranslated(formatTemplate)
    return remember(translatedTemplate, *args) {
        runCatching { String.format(translatedTemplate, *args) }.getOrDefault(formatTemplate)
    }
}

/**
 * Drop-in replacement for Material [Text] that automatically translates [text] via on-device ML Kit.
 * All style/layout params are forwarded to Material [Text] unchanged.
 */
@Composable
fun TranslatedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val t = rememberTranslated(text)
    Text(
        text = t,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}
