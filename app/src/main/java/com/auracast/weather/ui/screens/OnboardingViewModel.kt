package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracast.weather.data.llm.GemmaDownloadState
import com.auracast.weather.data.llm.GemmaModelDownloader
import com.auracast.weather.data.onboarding.OnboardingDataStore
import com.auracast.weather.data.translate.LanguageDownloadState
import com.auracast.weather.data.translate.TranslationPreferencesDataStore
import com.auracast.weather.data.translate.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val downloadState: GemmaDownloadState = GemmaDownloadState.Idle,
    val selectedLanguage: String = "en",
    val onboardingComplete: Boolean = false,
    val languageDownloadState: LanguageDownloadState = LanguageDownloadState.Idle,
    val languageDownloadStates: Map<String, LanguageDownloadState> = emptyMap(),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val downloader: GemmaModelDownloader,
    private val onboardingDataStore: OnboardingDataStore,
    private val translationRepository: TranslationRepository,
    private val translationPrefs: TranslationPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        // If the model is already present from a previous run, reflect that immediately
        // rather than making the user tap Download again.
        if (downloader.isModelDownloaded()) {
            _uiState.value = _uiState.value.copy(downloadState = GemmaDownloadState.Complete(downloader.modelFile()))
        }
        // Re-entering onboarding after it was already completed (e.g. process death mid-navigation)
        // should skip straight through rather than re-running the whole flow.
        viewModelScope.launch {
            if (onboardingDataStore.isCompleteFlow.first()) {
                _uiState.value = _uiState.value.copy(onboardingComplete = true)
            }
        }
        // Keep selected language in sync with persisted prefs (survives process death / app restart).
        viewModelScope.launch {
            translationPrefs.languageCodeFlow.collect { code ->
                _uiState.value = _uiState.value.copy(selectedLanguage = code)
            }
        }
        // Mirror repository download states so LanguagePage can show per-language spinner/check/warning.
        viewModelScope.launch {
            translationRepository.downloadStates.collect { map ->
                val selected = _uiState.value.selectedLanguage
                _uiState.value = _uiState.value.copy(
                    languageDownloadStates = map,
                    languageDownloadState = map[selected] ?: LanguageDownloadState.Idle
                )
            }
        }
        // Prime the download-state map for all launch languages
        viewModelScope.launch { translationRepository.refreshAllDownloadStates() }
    }

    fun startGemmaDownload(allowCellular: Boolean = false) {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            downloader.download(allowCellular).collect { state ->
                _uiState.value = _uiState.value.copy(downloadState = state)
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloader.cancelPartialDownload()
        _uiState.value = _uiState.value.copy(downloadState = GemmaDownloadState.Idle)
    }

    fun onLanguageSelected(languageCode: String) {
        viewModelScope.launch {
            translationRepository.setLanguage(languageCode)
            // Kick off ML Kit pack download — wifiOnly=true by default to respect data plans.
            // If it hits RequiresWifi, the UI shows a "Use cellular anyway" button that calls retryWithCellular().
            _uiState.value = _uiState.value.copy(selectedLanguage = languageCode)
            if (languageCode != "en") {
                translationRepository.ensureModelDownloaded(languageCode, wifiOnly = true)
            }
        }
    }

    fun retryLanguageDownloadWithCellular() {
        val code = _uiState.value.selectedLanguage
        if (code == "en") return
        viewModelScope.launch { translationRepository.ensureModelDownloaded(code, wifiOnly = false) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            // Persist selected language before marking onboarding complete so the app can
            // immediately start translating every visible string on first Home render.
            val code = _uiState.value.selectedLanguage
            if (code != translationPrefs.languageCodeFlow.first()) {
                translationRepository.setLanguage(code)
            }
            onboardingDataStore.markComplete()
            _uiState.value = _uiState.value.copy(onboardingComplete = true)
        }
    }
}
