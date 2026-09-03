package com.auracast.weather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auracast.weather.data.translate.LanguageDownloadState
import com.auracast.weather.data.translate.TranslationManager
import com.auracast.weather.data.tts.VoiceOption
import com.auracast.weather.ui.components.rememberTranslated

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
        Text(rememberTranslated("Settings"), style = MaterialTheme.typography.headlineSmall)

        // Units
        Text(rememberTranslated("Units"), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(rememberTranslated("°C / km/h / mm"))
            Switch(checked = state.useMetric, onCheckedChange = { viewModel.setMetric(it) })
        }

        // Appearance (Phase 11)
        Text(rememberTranslated("Appearance"), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("System", "Light", "Dark").forEach { mode ->
                Button(onClick = { viewModel.setThemeMode(mode) }) { Text(rememberTranslated(mode)) }
            }
        }

        // Language (Phase 7 — free on-device ML Kit, downloadable packs, changeable anytime)
        Text(rememberTranslated("Language"), style = MaterialTheme.typography.titleSmall)
        Text(rememberTranslated("Current: ${state.languageDisplay} — ${state.translationStatus}"))
        Text(
            rememberTranslated("Every piece of text in the app translates on-device to your chosen language via free ML Kit packs (~30 MB each). Works offline after first download."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.openLanguagePicker() }) { Text(rememberTranslated("Change language")) }
            // Quick retry if currently in a failed/wifi-required state
            val code = state.languageCode
            val st = state.languageDownloadStates[code]
            if (st is LanguageDownloadState.RequiresWifi) {
                OutlinedButton(onClick = { viewModel.retryLanguageDownload(code, allowCellular = true) }) { Text(rememberTranslated("Use cellular")) }
            } else if (st is LanguageDownloadState.Failed || st is LanguageDownloadState.NotDownloaded) {
                OutlinedButton(onClick = { viewModel.retryLanguageDownload(code) }) { Text(rememberTranslated("Download")) }
            }
        }

        if (state.showLanguagePicker) {
            LanguagePickerDialog(
                currentCode = state.languageCode,
                states = state.languageDownloadStates,
                onSelect = { viewModel.selectLanguage(it) },
                onRetryWifi = { viewModel.retryLanguageDownload(it, allowCellular = true) },
                onRetry = { viewModel.retryLanguageDownload(it) },
                onDelete = { viewModel.deleteLanguageModel(it) },
                onDismiss = { viewModel.dismissLanguagePicker() }
            )
        }

        // Voice (Phase 6) — pick and preview the on-device TTS voice used for AI reports
        Text(rememberTranslated("AI Report Voice"), style = MaterialTheme.typography.titleSmall)
        when {
            state.voicesLoading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            state.voices.isEmpty() -> Text(
                rememberTranslated("No extra voices found for your language — the system default will be used."),
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
        Text(rememberTranslated("Notifications"), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(rememberTranslated("Severe weather alerts"))
            Switch(checked = state.severeAlertsEnabled, onCheckedChange = { viewModel.setSevereAlerts(it) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(rememberTranslated("Daily briefing at ${state.briefingTime}"))
            Switch(checked = state.dailyBriefingEnabled, onCheckedChange = { viewModel.setDailyBriefing(it) })
        }

        // AI quality toggle (Phase 5 — E2B vs E4B)
        Text(rememberTranslated("AI Report quality"), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(rememberTranslated("Higher quality (E4B, needs 6GB RAM)"))
            Switch(checked = state.preferE4B, onCheckedChange = { viewModel.setPreferE4B(it) })
        }

        // Subscription (Phase 10)
        Text(rememberTranslated("Subscription"), style = MaterialTheme.typography.titleSmall)
        Text(rememberTranslated(if (state.isAdFree) "AuraCast Plus — ad-free ✓" else "Free — ads shown"))
        Button(onClick = { viewModel.onBillingAction() }) {
            Text(rememberTranslated(if (state.isAdFree) "Manage subscription" else "Upgrade to Plus"))
        }
        Button(onClick = { viewModel.restorePurchases() }) { Text(rememberTranslated("Restore purchases")) }

        Text(rememberTranslated("About — AuraCast Weather v0.1.0  •  Privacy policy at /privacy"), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LanguagePickerDialog(
    currentCode: String,
    states: Map<String, LanguageDownloadState>,
    onSelect: (String) -> Unit,
    onRetryWifi: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(rememberTranslated("Choose language")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(rememberTranslated("All languages use free on-device ML Kit packs (~30 MB). One download, then fully offline."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TranslationManager.LAUNCH_LANGUAGES.forEach { (code, label) ->
                    val isSelected = code == currentCode
                    val st = states[code]
                    val statusText = when (st) {
                        is LanguageDownloadState.Downloading -> rememberTranslated("Downloading…")
                        is LanguageDownloadState.Ready -> rememberTranslated("✓ Offline ready")
                        is LanguageDownloadState.RequiresWifi -> rememberTranslated("Needs Wi-Fi")
                        is LanguageDownloadState.Failed -> rememberTranslated("Failed — tap retry")
                        is LanguageDownloadState.NotDownloaded -> rememberTranslated("Not downloaded")
                        else -> if (code == "en") rememberTranslated("Ready") else rememberTranslated("Tap to download")
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(code) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else null)
                            Text(statusText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        when (st) {
                            is LanguageDownloadState.Downloading -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            is LanguageDownloadState.Ready -> if (code != "en") {
                                IconButton(onClick = { onDelete(code) }) { Icon(Icons.Filled.Delete, contentDescription = rememberTranslated("Delete pack")) }
                            }
                            is LanguageDownloadState.RequiresWifi -> TextButton(onClick = { onRetryWifi(code) }) { Text(rememberTranslated("Use cellular"), style = MaterialTheme.typography.labelSmall) }
                            is LanguageDownloadState.Failed, is LanguageDownloadState.NotDownloaded -> TextButton(onClick = { onRetry(code) }) { Text(rememberTranslated("Download"), style = MaterialTheme.typography.labelSmall) }
                            else -> {}
                        }
                    }
                }
                // Free-tier reassurance footer
                Box(Modifier.fillMaxWidth().padding(top = 6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Text(rememberTranslated("ML Kit translation is free, on-device, and needs no API key. Delete any pack in this list to reclaim ~30 MB."), style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(rememberTranslated("Close")) } }
    )
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
                "${voice.localeDisplay} • ${qualityLabel(voice.quality)} • ${if (voice.offline) rememberTranslated("Offline") else rememberTranslated("Needs network")}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (previewing) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        } else {
            IconButton(onClick = onPreview) {
                Icon(Icons.Filled.PlayArrow, contentDescription = rememberTranslated("Preview ${voiceDisplayName(voice)}"))
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
