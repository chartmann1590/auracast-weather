package com.auracast.weather.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auracast.weather.ui.components.WeatherIcon
import com.auracast.weather.ui.components.rememberTranslated

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isGenerating) {
        if (!state.isGenerating && state.script.isNotEmpty()) {
            (context as? Activity)?.let { activity ->
                adsViewModel.adManager.maybeShowInterstitial(activity)
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(rememberTranslated("AI Weather Podcast"), style = MaterialTheme.typography.headlineSmall)
        WeatherIcon(wmoCode = state.wmoCode, isDay = !state.isNight)

        Button(onClick = { viewModel.generateReport() }, enabled = !state.isGenerating) {
            Text(rememberTranslated(if (state.script.isEmpty()) "Generate Report" else "Regenerate"))
        }

        if (state.isGenerating) {
            Text(rememberTranslated("Generating… ${state.tokens} tokens"), style = MaterialTheme.typography.bodySmall)
        }

        // The AI report itself (Gemma output) is English by default — per Phase 7 it can be either
        // prompted directly in the target language or run through ML Kit translation. We apply the
        // ML Kit path here so the report always matches the user's chosen language once its pack is ready.
        val rawScript = state.script.ifEmpty { "Tap Generate to create your on-device AI weather report (Gemma 4)." }
        val displayScript = rememberTranslated(rawScript)
        Text(
            text = displayScript,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { viewModel.togglePlayback() }) {
                Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = rememberTranslated("Play/Pause"))
            }
            Slider(
                value = state.playbackPosition,
                onValueChange = { viewModel.seek(it) },
                modifier = Modifier.weight(1f)
            )
            Text("${(state.playbackPosition * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
        }

        if (state.showOfflineNudge) {
            Text(
                rememberTranslated("For fully offline podcasts, install an offline voice: Settings → Accessibility → Text-to-speech → Download voice data."),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (state.engineName.isNotEmpty()) {
            Text(rememberTranslated("Engine: ${state.engineName}"), style = MaterialTheme.typography.labelSmall)
        }
    }
}
