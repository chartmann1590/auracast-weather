package com.auracast.weather.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.auracast.weather.data.location.LocationSearchViewModel
import com.auracast.weather.ui.components.HomeBannerAd
import com.auracast.weather.ui.components.WeatherIcon
import com.auracast.weather.ui.components.WeatherIconSmall
import com.auracast.weather.ui.components.rememberTranslated
import com.auracast.weather.ui.theme.WeatherPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: LocationSearchViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val palette = WeatherPalette.forCode(uiState.wmoCode, isNight = uiState.isNight)
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val gradient = Brush.verticalGradient(
        colors = if (isDarkTheme) listOf(palette.darkStart, palette.darkEnd)
        else listOf(palette.lightStart, palette.lightEnd)
    )
    val context = LocalContext.current
    var showSearch by remember { mutableStateOf(false) }
    val query by searchViewModel.query.collectAsState()
    val results by searchViewModel.results.collectAsState()
    val recent by searchViewModel.recent.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted()
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            viewModel.onPermissionGranted()
        }
    }

    Box(Modifier.fillMaxSize().background(gradient)) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.hourly.isNotEmpty(),
            onRefresh = { viewModel.onPullToRefresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                searchViewModel.onQueryChange(it)
                                showSearch = it.length >= 2
                            },
                            placeholder = { Text(rememberTranslated("Search city or zip…")) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White.copy(alpha = 0.18f),
                                focusedContainerColor = Color.White.copy(alpha = 0.28f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (showSearch && results.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Column {
                                    results.take(6).forEach { loc ->
                                        Text(
                                            text = loc.label,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    searchViewModel.onSelect(loc)
                                                    showSearch = false
                                                    searchViewModel.onQueryChange("")
                                                }
                                                .padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        } else if (showSearch && query.length >= 2 && results.isEmpty()) {
                            Text(rememberTranslated("No results — try another city name"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                        }
                        if (!showSearch && recent.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                items(recent) { r ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier.clickable { searchViewModel.onQueryChange(r); showSearch = true },
                                    ) {
                                        Text(r, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (uiState.isLoading && uiState.hourly.isEmpty()) {
                            CircularProgressIndicator(color = Color.White)
                            Text(rememberTranslated(uiState.conditionText), style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(top = 12.dp))
                            Text(uiState.locationLabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        } else {
                            Text(
                                uiState.locationLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                            WeatherIcon(wmoCode = uiState.wmoCode, isDay = !uiState.isNight)
                            Text(
                                text = "${uiState.currentTemp}°",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Text(text = rememberTranslated(uiState.conditionText), style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.95f))
                            Text(
                                text = rememberTranslated("H ${uiState.highTemp}°  L ${uiState.lowTemp}°"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                            if (uiState.showCachedBanner) {
                                Text(
                                    text = rememberTranslated("Showing cached data from ${uiState.cachedAgeMinutes}m ago"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            )
                            .padding(top = 20.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(rememberTranslated("Your weather, narrated →"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(rememberTranslated(uiState.reportTeaser), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Text(
                            rememberTranslated("Hourly • 48h"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        if (uiState.hourly.isEmpty() && !uiState.isLoading) {
                            Text(rememberTranslated("Hourly data unavailable"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.hourly) { hour ->
                                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(rememberTranslated(hour.timeLabel), style = MaterialTheme.typography.labelSmall)
                                            WeatherIconSmall(wmoCode = uiState.wmoCode, isDay = !uiState.isNight)
                                            Text("${hour.temp}°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            if (hour.precipProb > 0) Text("${hour.precipProb}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            rememberTranslated("5-Day Forecast"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        uiState.daily.forEach { day ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(rememberTranslated(day.dayName), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    WeatherIconSmall(wmoCode = uiState.wmoCode, isDay = true)
                                    Text(
                                        "${day.high}° / ${day.low}°",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                        }

                        if (uiState.daily.isEmpty() && !uiState.isLoading) {
                            Text(rememberTranslated("Daily forecast unavailable — pull to refresh"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                        }

                        HomeBannerAd(adsViewModel.adManager, adsViewModel.billingManager)
                    }
                }
            }
        }
    }
}
