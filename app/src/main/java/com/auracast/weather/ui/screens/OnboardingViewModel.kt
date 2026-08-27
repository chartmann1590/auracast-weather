package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracast.weather.data.llm.GemmaDownloadState
import com.auracast.weather.data.llm.GemmaModelDownloader
import com.auracast.weather.data.onboarding.OnboardingDataStore
import com.auracast.weather.data.translate.TranslationManager
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
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val downloader: GemmaModelDownloader,
    private val onboardingDataStore: OnboardingDataStore,
    private val translationManager: TranslationManager,
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
        _uiState.value = _uiState.value.copy(selectedLanguage = languageCode)
        if (languageCode != "en") {
            viewModelScope.launch {
                translationManager.ensureModelDownloaded(languageCode, wifiOnly = true)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingDataStore.markComplete()
            _uiState.value = _uiState.value.copy(onboardingComplete = true)
        }
    }
}
