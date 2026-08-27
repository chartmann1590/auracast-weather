package com.auracast.weather.data.location

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.locationDataStore by preferencesDataStore(name = "location_prefs")

@Singleton
class LocationDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val KEY_LAST_LOCATION = stringPreferencesKey("last_location_json")
    private val KEY_SAVED_LOCATIONS = stringPreferencesKey("saved_locations_json")
    private val KEY_RECENT_SEARCHES = stringSetPreferencesKey("recent_searches")

    suspend fun saveLastLocation(location: ResolvedLocation) {
        context.locationDataStore.edit { prefs ->
            prefs[KEY_LAST_LOCATION] = json.encodeToString(location)
        }
    }

    val lastLocationFlow: Flow<ResolvedLocation?> = context.locationDataStore.data.map { prefs ->
        prefs[KEY_LAST_LOCATION]?.let { runCatching { json.decodeFromString<ResolvedLocation>(it) }.getOrNull() }
    }

    suspend fun addRecentSearch(query: String) {
        context.locationDataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_SEARCHES] ?: emptySet()
            prefs[KEY_RECENT_SEARCHES] = (setOf(query) + current).take(5).toSet()
        }
    }

    val recentSearchesFlow: Flow<List<String>> = context.locationDataStore.data.map { it[KEY_RECENT_SEARCHES]?.toList() ?: emptyList() }
}
