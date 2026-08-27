package com.auracast.weather.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracast.weather.data.llm.AiReportEngineSelector
import com.auracast.weather.data.tts.TtsRepository
import com.auracast.weather.data.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val script: String = "",
    val isGenerating: Boolean = false,
    val tokens: Int = 0,
    val isPlaying: Boolean = false,
    val playbackPosition: Float = 0f,
    val showOfflineNudge: Boolean = false,
    val engineName: String = "",
    val wmoCode: Int = 0,
    val isNight: Boolean = false,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val engineSelector: AiReportEngineSelector,
    private val tts: TtsRepository,
    private val weatherRepository: WeatherRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun generateReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, tokens = 0, script = "")
            val snapshot = weatherRepository.weatherFlow.value
            val prompt = snapshot?.let { buildPrompt(it) } ?: "Weather report for unknown location"
            _uiState.value = _uiState.value.copy(
                wmoCode = snapshot?.wmoCode ?: 0,
                isNight = snapshot?.isNight ?: false,
            )
            val engine = engineSelector.select()
            _uiState.value = _uiState.value.copy(engineName = engine.name)
            try {
                engine.generateReport(prompt).collect { chunk ->
                    _uiState.value = _uiState.value.copy(
                        script = _uiState.value.script + chunk,
                        tokens = _uiState.value.tokens + 1
                    )
                }
            } finally {
                _uiState.value = _uiState.value.copy(isGenerating = false)
            }
        }
    }

    private fun buildPrompt(snapshot: com.auracast.weather.data.weather.WeatherSnapshot): String {
        // Compact structured context — Phase 5 prompt design
        return buildString {
            appendLine("System: You are a friendly, upbeat local weather reporter recording a daily podcast segment. Use the exact numbers given. Write 275-350 words — a full, detailed segment, not a summary. Speak naturally, like a radio host, not like a data table.")
            appendLine("Output ONLY the words the host actually speaks aloud. Do not include stage directions, sound effect cues, music cues, or any bracketed/asterisked notes like \"(upbeat music fades in)\" or \"**music swells**\" — no markdown formatting or asterisks at all, just plain spoken sentences.")
            appendLine()
            appendLine("Context:")
            appendLine("Location: ${snapshot.locationLabel}")
            appendLine("Current: ${snapshot.currentTempF}°F, feels like ${snapshot.feelsLikeF}°F, ${snapshot.conditionText}, humidity ${snapshot.humidity}%, wind ${snapshot.windMph}mph")
            appendLine("Today high/low: ${snapshot.todayHighF}°F / ${snapshot.todayLowF}°F")
            appendLine("Hourly trend: ${snapshot.hourlyTrendSummary}")
            appendLine("Tomorrow: ${snapshot.tomorrowSummary}")
            appendLine()
            appendLine("Task: Write the podcast script now.")
        }
    }

    fun togglePlayback() {
        viewModelScope.launch {
            val s = _uiState.value
            if (s.isPlaying) {
                tts.stop()
                _uiState.value = s.copy(isPlaying = false)
            } else {
                if (s.script.isNotEmpty()) {
                    val offline = tts.isOfflineVoiceAvailable()
                    _uiState.value = s.copy(showOfflineNudge = !offline, isPlaying = true, playbackPosition = 0f)
                    tts.speak(
                        text = s.script,
                        onProgress = { pos -> _uiState.value = _uiState.value.copy(playbackPosition = pos) },
                        // Bug fix: playback previously never returned the button to "play" —
                        // isPlaying was only ever flipped back on a manual pause tap.
                        onComplete = { _uiState.value = _uiState.value.copy(isPlaying = false, playbackPosition = 1f) },
                    )
                }
            }
        }
    }

    fun seek(fraction: Float) {
        _uiState.value = _uiState.value.copy(playbackPosition = fraction)
        tts.seek(fraction)
    }

    override fun onCleared() {
        tts.stop()
        super.onCleared()
    }
}
