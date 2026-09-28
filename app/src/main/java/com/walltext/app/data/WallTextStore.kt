package com.walltext.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.walltext.app.model.WallpaperConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.store by preferencesDataStore(name = "walltext")

class WallTextStore(
    private val context: Context
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val currentKey = stringPreferencesKey("current")
    private val historyKey = stringPreferencesKey("history")

    val current: Flow<WallpaperConfig?> =
        context.store.data.map { preferences ->
            preferences[currentKey]?.let { stored ->
                json.decodeFromString<WallpaperConfig>(stored)
            }
        }

    val history: Flow<List<WallpaperConfig>> =
        context.store.data.map { preferences ->
            preferences[historyKey]?.let { stored ->
                json.decodeFromString<List<WallpaperConfig>>(stored)
            } ?: emptyList()
        }

    suspend fun save(config: WallpaperConfig) {
        context.store.updateData { preferences ->

            val oldHistory =
                preferences[historyKey]?.let { stored ->
                    json.decodeFromString<List<WallpaperConfig>>(stored)
                } ?: emptyList()

            val newHistory =
                (listOf(config) + oldHistory.filterNot { it.id == config.id })
                    .take(10)

            preferences.toMutablePreferences().apply {
                this[currentKey] = json.encodeToString(config)
                this[historyKey] = json.encodeToString(newHistory)
            }
        }
    }
}