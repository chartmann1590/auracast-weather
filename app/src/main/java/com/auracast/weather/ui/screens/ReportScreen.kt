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

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Phase 10 — natural transition point: right after a report finishes generating,
    // not a random interruption. AdManager's own cadence caps (once/session, 3-min gap)
    // decide whether this actually shows anything.
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
            .padding(bottom = 72.dp), // clears the bottom nav bar so play controls are always reachable
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Weather Podcast", style = MaterialTheme.typography.headlineSmall)
        // "Album art" — same animated Meteocons icon as Home, shared-visual-language
        // reinforcement of the podcast metaphor (Phase 16 §5.3).
        WeatherIcon(wmoCode = state.wmoCode, isDay = !state.isNight)

        Button(onClick = { viewModel.generateReport() }, enabled = !state.isGenerating) {
            Text(if (state.script.isEmpty()) "Generate Report" else "Regenerate")
        }

        if (state.isGenerating) {
            Text("Generating… ${state.tokens} tokens", style = MaterialTheme.typography.bodySmall)
        }

        Text(
            text = state.script.ifEmpty { "Tap Generate to create your on-device AI weather report (Gemma 4)." },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { viewModel.togglePlayback() }) {
                Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause")
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
                "For fully offline podcasts, install an offline voice: Settings → Accessibility → Text-to-speech → Download voice data.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (state.engineName.isNotEmpty()) {
            Text("Engine: ${state.engineName}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
