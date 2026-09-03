package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracast.weather.data.translate.LanguageDownloadState
import com.auracast.weather.data.translate.TranslationManager
import com.auracast.weather.data.translate.TranslationPreferencesDataStore
import com.auracast.weather.data.translate.TranslationRepository
import com.auracast.weather.data.tts.TtsRepository
import com.auracast.weather.data.tts.VoiceOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val useMetric: Boolean = false,
    val themeMode: String = "System",
    val languageCode: String = "en",
    val languageDisplay: String = "English",
    val translationStatus: String = "Ready",
    val languageDownloadStates: Map<String, LanguageDownloadState> = emptyMap(),
    val showLanguagePicker: Boolean = false,
    val severeAlertsEnabled: Boolean = false,
    val dailyBriefingEnabled: Boolean = false,
    val briefingTime: String = "07:00",
    val preferE4B: Boolean = false,
    val isAdFree: Boolean = false,
    val voices: List<VoiceOption> = emptyList(),
    val selectedVoiceName: String? = null,
    val voicesLoading: Boolean = true,
    val previewingVoiceName: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val translationRepository: TranslationRepository,
    private val translationPrefs: TranslationPreferencesDataStore,
    private val ttsRepository: TtsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ttsRepository.selectedVoiceNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(selectedVoiceName = name)
            }
        }
        viewModelScope.launch {
            translationPrefs.languageCodeFlow.collect { code ->
                val label = TranslationManager.labelFor(code)
                val status = when (val s = _uiState.value.languageDownloadStates[code]) {
                    is LanguageDownloadState.Downloading -> "Downloading…"
                    is LanguageDownloadState.Ready -> "Ready (offline)"
                    is LanguageDownloadState.RequiresWifi -> "Needs Wi-Fi"
                    is LanguageDownloadState.Failed -> "Failed"
                    is LanguageDownloadState.NotDownloaded -> "Not downloaded"
                    else -> if (code == "en") "Ready" else "Checking…"
                }
                _uiState.value = _uiState.value.copy(languageCode = code, languageDisplay = label, translationStatus = status)
            }
        }
        viewModelScope.launch {
            translationRepository.downloadStates.collect { map ->
                val code = _uiState.value.languageCode
                val status = when (val s = map[code]) {
                    is LanguageDownloadState.Downloading -> "Downloading…"
                    is LanguageDownloadState.Ready -> "Ready (offline)"
                    is LanguageDownloadState.RequiresWifi -> "Needs Wi-Fi — tap to retry"
                    is LanguageDownloadState.Failed -> "Failed — ${s.reason}"
                    is LanguageDownloadState.NotDownloaded -> "Not downloaded — tap to download"
                    else -> _uiState.value.translationStatus
                }
                _uiState.value = _uiState.value.copy(languageDownloadStates = map, translationStatus = status)
            }
        }
        viewModelScope.launch { translationRepository.refreshAllDownloadStates() }
        loadVoices()
    }

    private fun loadVoices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(voicesLoading = true)
            val voices = ttsRepository.getAvailableVoices()
            _uiState.value = _uiState.value.copy(voices = voices, voicesLoading = false)
        }
    }

    fun selectVoice(name: String) {
        viewModelScope.launch { ttsRepository.selectVoice(name) }
    }

    fun previewVoice(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(previewingVoiceName = name)
            ttsRepository.previewVoice(name)
            delay(3500) // engine.speak() is fire-and-forget; approximate the sample's spoken length so the "previewing" state is visible
            _uiState.value = _uiState.value.copy(previewingVoiceName = null)
        }
    }

    fun setMetric(metric: Boolean) { _uiState.value = _uiState.value.copy(useMetric = metric) }
    fun setThemeMode(mode: String) { _uiState.value = _uiState.value.copy(themeMode = mode) }
    fun openLanguagePicker() { _uiState.value = _uiState.value.copy(showLanguagePicker = true) }
    fun dismissLanguagePicker() { _uiState.value = _uiState.value.copy(showLanguagePicker = false) }

    fun selectLanguage(code: String) {
        viewModelScope.launch {
            translationRepository.setLanguage(code)
            // Immediately kick off free ML Kit download so the app flips to the new language as soon as pack is ready.
            if (code != "en") translationRepository.ensureModelDownloaded(code, wifiOnly = true)
        }
    }

    fun retryLanguageDownload(code: String, allowCellular: Boolean = false) {
        viewModelScope.launch { translationRepository.ensureModelDownloaded(code, wifiOnly = !allowCellular) }
    }

    fun deleteLanguageModel(code: String) {
        viewModelScope.launch { translationRepository.deleteModel(code) }
    }

    fun setSevereAlerts(enabled: Boolean) { _uiState.value = _uiState.value.copy(severeAlertsEnabled = enabled) }
    fun setDailyBriefing(enabled: Boolean) { _uiState.value = _uiState.value.copy(dailyBriefingEnabled = enabled) }
    fun setPreferE4B(prefer: Boolean) { _uiState.value = _uiState.value.copy(preferE4B = prefer) }
    fun onBillingAction() { /* Phase 10 */ }
    fun restorePurchases() { /* Phase 10 */ }
}
