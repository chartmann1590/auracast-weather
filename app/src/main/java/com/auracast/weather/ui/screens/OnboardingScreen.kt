package com.auracast.weather.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.auracast.weather.data.translate.AppLanguage
import com.auracast.weather.data.translate.LanguageDownloadState
import com.auracast.weather.data.translate.TranslationManager
import com.auracast.weather.ui.components.WeatherIcon
import com.auracast.weather.ui.components.rememberTranslated
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()

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
                    0 -> LanguagePage(
                        selected = state.selectedLanguage,
                        downloadStatus = state.languageDownloadState,
                        allDownloadStates = state.languageDownloadStates,
                        searchQuery = state.languageSearchQuery,
                        onSearchChange = viewModel::onLanguageSearchChange,
                        onSelect = viewModel::onLanguageSelected,
                        onRetryCellular = viewModel::retryLanguageDownloadWithCellular,
                        onNext = { scope.launch { pagerState.animateScrollToPage(1) } },
                    )
                    1 -> WelcomePage(
                        onNext = { scope.launch { pagerState.animateScrollToPage(2) } }
                    )
                    2 -> LocationPage(
                        onNext = { scope.launch { pagerState.animateScrollToPage(3) } }
                    )
                    3 -> AiPodcastDownloadPage(
                        downloadState = state.downloadState,
                        onDownload = { viewModel.startGemmaDownload() },
                        onDownloadOverCellular = { viewModel.startGemmaDownload(allowCellular = true) },
                        onSkip = viewModel::completeOnboarding,
                        onContinue = viewModel::completeOnboarding,
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
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            content()
        }
    }
}

@Composable
private fun LanguagePage(
    selected: String,
    downloadStatus: LanguageDownloadState,
    allDownloadStates: Map<String, LanguageDownloadState>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onRetryCellular: () -> Unit,
    onNext: () -> Unit,
) {
    val languages = TranslationManager.searchLanguages(searchQuery)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            rememberTranslated("Choose Your Language"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            rememberTranslated("Select your native language. AuraCast uses free on-device ML Kit to translate every screen."),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            rememberTranslated("Free ~30 MB pack downloads once, then works 100% offline."),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(rememberTranslated("Search 59 languages…")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.18f),
                focusedContainerColor = Color.White.copy(alpha = 0.28f),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Language Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(languages, key = { it.code }) { lang ->
                val isSelected = lang.code == selected
                val perLangState = allDownloadStates[lang.code]

                Card(
                    onClick = { onSelect(lang.code) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.18f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    ),
                ) {
                    Column(
                        Modifier.padding(vertical = 10.dp, horizontal = 4.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            lang.nativeName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        Text(
                            lang.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        when (perLangState) {
                            is LanguageDownloadState.Downloading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                            is LanguageDownloadState.Ready -> {
                                Text(
                                    if (lang.code == "en") rememberTranslated("✓ Ready") else rememberTranslated("✓ Offline"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
                                )
                            }
                            is LanguageDownloadState.RequiresWifi -> {
                                Text(rememberTranslated("Needs Wi-Fi"), style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                            is LanguageDownloadState.Failed -> {
                                Text(rememberTranslated("Retry"), style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Active language status bar
        when (downloadStatus) {
            is LanguageDownloadState.Downloading -> {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.size(8.dp))
                    Text(rememberTranslated("Downloading free ML Kit language pack…"), style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
            is LanguageDownloadState.RequiresWifi -> {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f), contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(rememberTranslated("Connect to Wi-Fi to download (~30 MB), or use cellular."), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRetryCellular) { Text(rememberTranslated("Use cellular anyway")) }
                            TextButton(onClick = onNext) { Text(rememberTranslated("Continue")) }
                        }
                    }
                }
            }
            is LanguageDownloadState.Failed -> {
                Text(
                    rememberTranslated("Download failed: ${downloadStatus.reason}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            is LanguageDownloadState.Ready -> {
                if (selected != "en") {
                    Text(
                        rememberTranslated("✓ Ready — all text translates on-device offline."),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            else -> {}
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(rememberTranslated("Continue"), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    OnboardingPageScaffold(
        title = rememberTranslated("Welcome to AuraCast"),
        subtitle = rememberTranslated("Live weather, animated radar, and a daily forecast narrated just for you — all built to run right on your phone."),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WeatherIcon(wmoCode = 1, isDay = true)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(0.75f).height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(rememberTranslated("Continue"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LocationPage(onNext: () -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onNext() }

    OnboardingPageScaffold(
        title = rememberTranslated("Know before you go"),
        subtitle = rememberTranslated("Share your location so we can show accurate current conditions and radar for right where you are. You can always search a city manually instead."),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                modifier = Modifier.fillMaxWidth(0.75f).height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(rememberTranslated("Enable Location"), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onNext, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text(rememberTranslated("I'll search manually"))
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
        title = rememberTranslated("Your weather, narrated"),
        subtitle = rememberTranslated("AuraCast uses Gemma 4, running fully on your device, to turn today's forecast into a short spoken briefing — like a personal weather podcast. It never leaves your phone."),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WeatherIcon(wmoCode = 95, isDay = true)
            Spacer(Modifier.height(20.dp))
            when (downloadState) {
                is GemmaDownloadState.Idle, is GemmaDownloadState.CheckingExisting -> {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth(0.85f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(rememberTranslated("Download AI voice (≈2.6 GB)"))
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onSkip, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text(rememberTranslated("Skip for now"))
                    }
                }
                is GemmaDownloadState.RequiresWifi -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(rememberTranslated("Connect to Wi-Fi to download the on-device AI model (about 2.6 GB)."), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onDownloadOverCellular) { Text(rememberTranslated("Use cellular anyway")) }
                                TextButton(onClick = onSkip) { Text(rememberTranslated("Skip for now")) }
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
                        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                            Text(rememberTranslated("Get Started"))
                        }
                    }
                }
                is GemmaDownloadState.Verifying -> {
                    Text(rememberTranslated("Verifying download…"), style = MaterialTheme.typography.bodySmall)
                }
                is GemmaDownloadState.Complete -> {
                    Text(rememberTranslated("✓ Ready — your AI voice is fully downloaded and works offline."), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(0.75f).height(48.dp)) {
                        Text(rememberTranslated("Get Started"))
                    }
                }
                is GemmaDownloadState.Failed -> {
                    Text(rememberTranslated("Download failed: ${downloadState.message}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onDownload) { Text(rememberTranslated("Retry")) }
                        TextButton(onClick = onSkip, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                            Text(rememberTranslated("Get Started"))
                        }
                    }
                }
            }
        }
    }
}
