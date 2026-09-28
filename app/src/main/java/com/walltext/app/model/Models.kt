package com.walltext.app.model

import kotlinx.serialization.Serializable

@Serializable enum class PlaceholderType { TEXT, TABLE, CHECKLIST, BULLETS }
@Serializable enum class DisplayMode { STATIC, DYNAMIC }

@Serializable
data class TextPlaceholder(
    val id: String,
    val title: String = "Text",
    val type: PlaceholderType = PlaceholderType.TEXT,
    val mode: DisplayMode = DisplayMode.STATIC,
    val entries: List<String> = listOf(""),
    val x: Float = .5f,
    val y: Float = .5f,
    val width: Float = .8f,
    val fontSizeSp: Float = 20f,
    val opacity: Float = 1f
) {
    fun normalizedEntries() = entries.filter { it.isNotBlank() }.take(10)
}

@Serializable
data class WallpaperConfig(
    val id: String,
    val wallpaperUri: String? = null,
    val wallpaperColor: Long = 0xFF101114,
    val placeholders: List<TextPlaceholder> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val dynamicIntervalMinutes: Int = 15
)
