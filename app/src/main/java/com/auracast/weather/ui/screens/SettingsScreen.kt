package com.auracast.weather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auracast.weather.data.tts.VoiceOption

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        // Units
        Text("Units", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("°C / km/h / mm")
            Switch(checked = state.useMetric, onCheckedChange = { viewModel.setMetric(it) })
        }

        // Appearance (Phase 11)
        Text("Appearance", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("System", "Light", "Dark").forEach { mode ->
                Button(onClick = { viewModel.setThemeMode(mode) }) { Text(mode) }
            }
        }

        // Language (Phase 7)
        Text("Language", style = MaterialTheme.typography.titleSmall)
        Text("Current: ${state.languageDisplay} — ${state.translationStatus}")
        Button(onClick = { viewModel.openLanguagePicker() }) { Text("Change language") }

        // Voice (Phase 6) — pick and preview the on-device TTS voice used for AI reports
        Text("AI Report Voice", style = MaterialTheme.typography.titleSmall)
        when {
            state.voicesLoading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            state.voices.isEmpty() -> Text(
                "No extra voices found for your language — the system default will be used.",
                style = MaterialTheme.typography.bodySmall,
            )
            else -> {
                // Distinct voices can still land on the same quality/network tier — number
                // them so two rows never render with an identical label.
                val seen = mutableMapOf<String, Int>()
                val labels = state.voices.associate { v ->
                    val base = voiceDisplayName(v)
                    val n = (seen[base] ?: 0) + 1
                    seen[base] = n
                    v.name to if (n > 1) "$base #$n" else base
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.voices.forEach { voice ->
                        VoiceRow(
                            voice = voice,
                            label = labels[voice.name].orEmpty(),
                            selected = voice.name == state.selectedVoiceName,
                            previewing = voice.name == state.previewingVoiceName,
                            onSelect = { viewModel.selectVoice(voice.name) },
                            onPreview = { viewModel.previewVoice(voice.name) },
                        )
                    }
                }
            }
        }

        // Notifications (Phase 8)
        Text("Notifications", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Severe weather alerts")
            Switch(checked = state.severeAlertsEnabled, onCheckedChange = { viewModel.setSevereAlerts(it) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Daily briefing at ${state.briefingTime}")
            Switch(checked = state.dailyBriefingEnabled, onCheckedChange = { viewModel.setDailyBriefing(it) })
        }

        // AI quality toggle (Phase 5 — E2B vs E4B)
        Text("AI Report quality", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Higher quality (E4B, needs 6GB RAM)")
            Switch(checked = state.preferE4B, onCheckedChange = { viewModel.setPreferE4B(it) })
        }

        // Subscription (Phase 10)
        Text("Subscription", style = MaterialTheme.typography.titleSmall)
        Text(if (state.isAdFree) "AuraCast Plus — ad-free ✓" else "Free — ads shown")
        Button(onClick = { viewModel.onBillingAction() }) {
            Text(if (state.isAdFree) "Manage subscription" else "Upgrade to Plus")
        }
        Button(onClick = { viewModel.restorePurchases() }) { Text("Restore purchases") }

        Text("About — AuraCast Weather v0.1.0  •  Privacy policy at /privacy", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun VoiceRow(
    voice: VoiceOption,
    label: String,
    selected: Boolean,
    previewing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp),
            )
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${voice.localeDisplay} • ${qualityLabel(voice.quality)} • ${if (voice.offline) "Offline" else "Needs network"}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (previewing) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        } else {
            IconButton(onClick = onPreview) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Preview ${voiceDisplayName(voice)}")
            }
        }
    }
}

// Engine voice names are cryptic IDs (e.g. "en-us-x-iol-network"); show something a
// non-technical user can actually compare at a glance instead.
private fun voiceDisplayName(voice: VoiceOption): String {
    val quality = qualityLabel(voice.quality)
    val kind = if (voice.offline) "Offline" else "Online"
    return "$quality Voice ($kind)"
}

private fun qualityLabel(quality: Int): String = when {
    quality >= 500 -> "Very High Quality"
    quality >= 400 -> "High Quality"
    quality >= 300 -> "Standard"
    else -> "Basic"
}
