package com.auracast.weather.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auracast.weather.data.llm.GemmaDownloadState
import com.auracast.weather.data.translate.TranslationManager
import com.auracast.weather.ui.components.WeatherIcon
import com.auracast.weather.ui.theme.WeatherPalette
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScopeCompat()

    LaunchedEffect(state.onboardingComplete) {
        if (state.onboardingComplete) onFinished()
    }

    val gradient = Brush.verticalGradient(
        listOf(WeatherPalette.clearDay.lightStart, WeatherPalette.partlyCloudy.lightEnd)
    )

    Box(Modifier.fillMaxSize().background(gradient)) {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> LocationPage(onNext = { scope.launch { pagerState.animateScrollToPage(2) } })
                    2 -> AiPodcastDownloadPage(
                        downloadState = state.downloadState,
                        onDownload = { viewModel.startGemmaDownload() },
                        onDownloadOverCellular = { viewModel.startGemmaDownload(allowCellular = true) },
                        onSkip = { scope.launch { pagerState.animateScrollToPage(3) } },
                        onContinue = { scope.launch { pagerState.animateScrollToPage(3) } },
                    )
                    3 -> LanguagePage(
                        selected = state.selectedLanguage,
                        onSelect = viewModel::onLanguageSelected,
                        onFinish = viewModel::completeOnboarding,
                    )
                }
            }

            // Page indicator dots
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(PAGE_COUNT) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 10.dp else 8.dp)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                CircleShape,
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()

@Composable
private fun OnboardingPageScaffold(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            // White at reduced opacity, not onSurfaceVariant — this screen sits directly on
            // the condition gradient (Phase 16 §5.1), not a plain Material surface, so the
            // default muted-gray text color was reading as nearly invisible on it.
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        // Covers every plain Text() inside page content that doesn't set its own color —
        // TextButton/Button still set their own explicit colors below and are unaffected.
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            content()
        }
    }
}

@Composable
private fun WelcomePage() {
    OnboardingPageScaffold(
        title = "Welcome to AuraCast",
        subtitle = "Live weather, animated radar, and a daily forecast narrated just for you — all built to run right on your phone.",
    ) {
        WeatherIcon(wmoCode = 1, isDay = true)
    }
}

@Composable
private fun LocationPage(onNext: () -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onNext() }

    OnboardingPageScaffold(
        title = "Know before you go",
        subtitle = "Share your location so we can show accurate current conditions and radar for right where you are. You can always search a city manually instead.",
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                Text("Enable Location")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNext, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text("I'll search manually")
            }
        }
    }
}

@Composable
private fun AiPodcastDownloadPage(
    downloadState: GemmaDownloadState,
    onDownload: () -> Unit,
    onDownloadOverCellular: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingPageScaffold(
        title = "Your weather, narrated",
        subtitle = "AuraCast uses Gemma 4, running fully on your device, to turn today's forecast into a short spoken briefing — like a personal weather podcast. It never leaves your phone.",
    ) {
        WeatherIcon(wmoCode = 95, isDay = true)
        Spacer(Modifier.height(24.dp))
        when (downloadState) {
            is GemmaDownloadState.Idle, is GemmaDownloadState.CheckingExisting -> {
                Button(onClick = onDownload) { Text("Download AI voice (≈2.6 GB)") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSkip, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("Skip for now")
                }
            }
            is GemmaDownloadState.RequiresWifi -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Connect to Wi-Fi to download the on-device AI model (about 2.6 GB).", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onDownloadOverCellular) { Text("Use cellular anyway") }
                            TextButton(onClick = onSkip) { Text("Skip for now") }
                        }
                    }
                }
            }
            is GemmaDownloadState.Downloading -> {
                val progress = if (downloadState.totalBytes > 0) {
                    (downloadState.bytesDownloaded.toFloat() / downloadState.totalBytes.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Column(Modifier.fillMaxWidth(0.8f), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    Spacer(Modifier.height(8.dp))
                    val mbDone = downloadState.bytesDownloaded / 1_000_000
                    val mbTotal = downloadState.totalBytes / 1_000_000
                    Text("${(progress * 100).toInt()}% • ${mbDone}MB / ${mbTotal}MB", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onSkip, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Continue without waiting")
                    }
                }
            }
            is GemmaDownloadState.Verifying -> {
                Text("Verifying download…", style = MaterialTheme.typography.bodySmall)
            }
            is GemmaDownloadState.Complete -> {
                Text("✓ Ready — your AI voice is fully downloaded and works offline.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onContinue) { Text("Continue") }
            }
            is GemmaDownloadState.Failed -> {
                Text("Download failed: ${downloadState.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onDownload) { Text("Retry") }
                    TextButton(onClick = onSkip, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Skip for now")
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePage(
    selected: String,
    onSelect: (String) -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "In your language",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick a language — your AI report and app text will translate on-device.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(TranslationManager.LAUNCH_LANGUAGES) { (code, label) ->
                val isSelected = code == selected
                // Translucent white, not colorScheme.surface — this grid sits on the raw
                // condition gradient (Phase 16 §5.1), and surface resolves near-black in
                // dark/dynamic themes, which read as broken solid-black tiles on the gradient.
                Card(
                    onClick = { onSelect(code) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.18f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    ),
                ) {
                    Box(Modifier.padding(vertical = 16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Get Started") }
        Spacer(Modifier.height(8.dp))
    }
}
