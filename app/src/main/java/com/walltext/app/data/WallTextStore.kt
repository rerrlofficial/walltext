package com.walltext.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.walltext.app.model.WallpaperConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.store by preferencesDataStore("walltext")

class WallTextStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val currentKey = stringPreferencesKey("current")
    private val historyKey = stringPreferencesKey("history")

    val current: Flow<WallpaperConfig?> = context.store.data.map { p -> p[currentKey]?.let { json.decodeFromString<WallpaperConfig>(it) } }
    val history: Flow<List<WallpaperConfig>> = context.store.data.map { p -> p[historyKey]?.let { json.decodeFromString<List<WallpaperConfig>>(it) } ?: emptyList() }

    suspend fun save(config: WallpaperConfig) {
        context.store.updateData { p ->
            val old = p[historyKey]?.let { json.decodeFromString<List<WallpaperConfig>>(it) } ?: emptyList()
            val newHistory = (listOf(config) + old.filterNot { it.id == config.id }).take(10)
            p.toMutablePreferences().apply {
                this[currentKey] = json.encodeToString(config)
                this[historyKey] = json.encodeToString(newHistory)
            }
        }
    }
}
