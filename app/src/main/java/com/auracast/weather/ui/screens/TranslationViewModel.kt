package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracast.weather.data.translate.AppLanguage
import com.auracast.weather.data.translate.LanguageDownloadState
import com.auracast.weather.data.translate.TranslationManager
import com.auracast.weather.data.translate.TranslationPreferencesDataStore
import com.auracast.weather.data.translate.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val repository: TranslationRepository,
    private val prefs: TranslationPreferencesDataStore,
    private val manager: TranslationManager,
) : ViewModel() {

    val currentLanguage: StateFlow<String> = prefs.languageCodeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val currentLabel: StateFlow<String> = prefs.languageLabelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "English")

    val downloadStates: StateFlow<Map<String, LanguageDownloadState>> = repository.downloadStates

    val revision: StateFlow<Long> = repository.revision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            repository.refreshAllDownloadStates()
        }
    }

    fun getCachedTranslation(text: String, target: String): String? =
        repository.getCachedTranslation(text, target)

    suspend fun translate(text: String): String = repository.translate(text)

    suspend fun translateTo(text: String, target: String): String = repository.translateTo(text, target)

    fun setLanguage(code: String) {
        viewModelScope.launch {
            repository.setLanguage(code)
        }
    }

    /** Called from onboarding/Settings when user taps a language. */
    fun ensureModelDownloaded(code: String, wifiOnly: Boolean = false) {
        viewModelScope.launch {
            repository.ensureModelDownloaded(code, wifiOnly)
        }
    }

    fun deleteModel(code: String) {
        viewModelScope.launch { repository.deleteModel(code) }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAllDownloadStates()
            _isRefreshing.value = false
        }
    }

    fun searchLanguages(query: String): List<AppLanguage> =
        TranslationManager.searchLanguages(query)
}
