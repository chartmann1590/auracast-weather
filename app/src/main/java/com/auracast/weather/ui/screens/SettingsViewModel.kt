package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracast.weather.data.translate.TranslationManager
import com.auracast.weather.data.tts.TtsRepository
import com.auracast.weather.data.tts.VoiceOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val useMetric: Boolean = false,
    val themeMode: String = "System",
    val languageDisplay: String = "English",
    val translationStatus: String = "Ready",
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
    private val translationManager: TranslationManager,
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
    fun openLanguagePicker() { /* TODO Phase 7: show language sheet */ }
    fun setSevereAlerts(enabled: Boolean) { _uiState.value = _uiState.value.copy(severeAlertsEnabled = enabled) }
    fun setDailyBriefing(enabled: Boolean) { _uiState.value = _uiState.value.copy(dailyBriefingEnabled = enabled) }
    fun setPreferE4B(prefer: Boolean) { _uiState.value = _uiState.value.copy(preferE4B = prefer) }
    fun onBillingAction() { /* Phase 10 */ }
    fun restorePurchases() { /* Phase 10 */ }
}
