package com.walltext.app

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.walltext.app.data.WallTextStore
import com.walltext.app.model.DisplayMode
import com.walltext.app.model.PlaceholderType
import com.walltext.app.model.TextPlaceholder
import com.walltext.app.model.WallpaperConfig
import com.walltext.app.ui.Theme
import com.walltext.app.wallpaper.WallpaperRenderer
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Theme {
                WallTextApp(this)
            }
        }
    }
}

@Composable
private fun WallTextApp(context: Context) {

    val store = remember {
        WallTextStore(context)
    }

    val scope = rememberCoroutineScope()

    var config by remember {
        mutableStateOf(
            WallpaperConfig(
                id = UUID.randomUUID().toString()
            )
        )
    }

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    val history by store.history.collectAsState(
        initial = emptyList()
    )

    val picker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {

                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                config = config.copy(
                    wallpaperUri = uri.toString()
                )
            }
        }

    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    Text("WallText")
                },
                actions = {

                    TextButton(
                        onClick = {

                            scope.launch {
                                store.save(config)
                                applyWallpaper(
                                    context,
                                    config
                                )
                            }
                        }
                    ) {
                        Text("Apply")
                    }
                }
            )
        },

        bottomBar = {

            NavigationBar {

                val tabs = listOf(
                    "Wallpaper",
                    "Editor",
                    "History",
                    "Settings"
                )

                tabs.forEachIndexed { index, title ->

                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                        },
                        icon = {},
                        label = {
                            Text(title)
                        }
                    )
                }
            }
        }

    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            when (selectedTab) {

                0 -> WallpaperTab(
                    config = config,
                    onChange = { newConfig ->
                        config = newConfig
                    },
                    onPick = {
                        picker.launch(
                            arrayOf("image/*")
                        )
                    }
                )

                1 -> EditorTab(
                    config = config,
                    onChange = { newConfig ->
                        config = newConfig
                    }
                )

                2 -> HistoryTab(
                    items = history,
                    onUse = { oldConfig ->
                        config = oldConfig
                    }
                )

                3 -> SettingsTab(
                    config = config,
                    onChange = { newConfig ->
                        config = newConfig
                    }
                )
            }
        }
    }
}

@Composable
private fun WallpaperTab(
    config: WallpaperConfig,
    onChange: (WallpaperConfig) -> Unit,
    onPick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Wallpaper",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                if (config.wallpaperUri != null) {

                    AsyncImage(
                        model = config.wallpaperUri,
                        contentDescription = "Selected wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(config.wallpaperColor)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "No wallpaper selected",
                            color = Color.White
                        )
                    }
                }
            }
        }

        Button(
            onClick = onPick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Choose wallpaper")
        }

        OutlinedButton(
            onClick = {

                onChange(
                    config.copy(
                        wallpaperUri = null,
                        wallpaperColor = 0xFF101114
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use solid background")
        }
    }
}

@Composable
private fun EditorTab(
    config: WallpaperConfig,
    onChange: (WallpaperConfig) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Text placeholders",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Up to 5 placeholders can be placed on the wallpaper.",
            style = MaterialTheme.typography.bodyMedium
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Color(0xFF101114),
                    RoundedCornerShape(20.dp)
                )
        ) {

            config.placeholders.forEachIndexed { index, placeholder ->

                val xOffset =
                    (placeholder.x * 320f - 160f).dp

                val yOffset =
                    (placeholder.y * 320f - 20f).dp

                Text(
                    text = previewText(placeholder),
                    color = Color.White.copy(
                        alpha = placeholder.opacity
                    ),
                    fontSize = placeholder.fontSizeSp.sp,
                    modifier = Modifier
                        .fillMaxWidth(placeholder.width)
                        .offset(
                            x = xOffset,
                            y = yOffset
                        )
                        .pointerInput(placeholder.id) {

                            detectDragGestures { change, dragAmount ->

                                change.consume()

                                val newX =
                                    (
                                        placeholder.x +
                                            dragAmount.x / 320f
                                        ).coerceIn(0f, 1f)

                                val newY =
                                    (
                                        placeholder.y +
                                            dragAmount.y / 320f
                                        ).coerceIn(0f, 1f)

                                val updated =
                                    config.placeholders.mapIndexed { itemIndex, item ->

                                        if (itemIndex == index) {

                                            item.copy(
                                                x = newX,
                                                y = newY
                                            )

                                        } else {
                                            item
                                        }
                                    }

                                onChange(
                                    config.copy(
                                        placeholders = updated
                                    )
                                )
                            }
                        }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        config.placeholders.forEachIndexed { index, placeholder ->

            PlaceholderCard(
                index = index,
                placeholder = placeholder,

                onChange = { newPlaceholder ->

                    val updated =
                        config.placeholders.mapIndexed { itemIndex, item ->

                            if (itemIndex == index) {
                                newPlaceholder
                            } else {
                                item
                            }
                        }

                    onChange(
                        config.copy(
                            placeholders = updated
                        )
                    )
                },

                onDelete = {

                    val updated =
                        config.placeholders.filterIndexed { itemIndex, _ ->
                            itemIndex != index
                        }

                    onChange(
                        config.copy(
                            placeholders = updated
                        )
                    )
                }
            )
        }

        if (config.placeholders.size < 5) {

            Button(
                onClick = {

                    val newPlaceholder =
                        TextPlaceholder(
                            id = UUID.randomUUID().toString(),
                            title = "Placeholder ${config.placeholders.size + 1}"
                        )

                    onChange(
                        config.copy(
                            placeholders =
                                config.placeholders + newPlaceholder
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add placeholder")
            }
        }
    }
}

@Composable
private fun PlaceholderCard(
    index: Int,
    placeholder: TextPlaceholder,
    onChange: (TextPlaceholder) -> Unit,
    onDelete: () -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = placeholder.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {
                    Text(
                        if (expanded) "Close"
                        else "Edit"
                    )
                }

                TextButton(
                    onClick = onDelete
                ) {
                    Text("Delete")
                }
            }

            if (expanded) {

                OutlinedTextField(
                    value = placeholder.title,
                    onValueChange = { value ->
                        onChange(
                            placeholder.copy(
                                title = value
                            )
                        )
                    },
                    label = {
                        Text("Name")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Type")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    PlaceholderType.values().forEach { type ->

                        FilterChip(
                            selected = placeholder.type == type,
                            onClick = {
                                onChange(
                                    placeholder.copy(
                                        type = type
                                    )
                                )
                            },
                            label = {
                                Text(
                                    type.name
                                        .lowercase()
                                        .replaceFirstChar { character ->
                                            character.uppercase()
                                        }
                                )
                            }
                        )
                    }
                }

                Text("Display mode")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    FilterChip(
                        selected = placeholder.mode == DisplayMode.STATIC,
                        onClick = {
                            onChange(
                                placeholder.copy(
                                    mode = DisplayMode.STATIC
                                )
                            )
                        },
                        label = {
                            Text("Static")
                        }
                    )

                    FilterChip(
                        selected = placeholder.mode == DisplayMode.DYNAMIC,
                        onClick = {
                            onChange(
                                placeholder.copy(
                                    mode = DisplayMode.DYNAMIC
                                )
                            )
                        },
                        label = {
                            Text("Dynamic")
                        }
                    )
                }

                if (placeholder.mode == DisplayMode.STATIC) {

                    OutText(
                        value = placeholder.entries.firstOrNull()
                            ?: "",
                        onValueChange = { value ->

                            onChange(
                                placeholder.copy(
                                    entries = listOf(value)
                                )
                            )
                        }
                    )

                } else {

                    Text("Dynamic entries (maximum 10)")

                    placeholder.entries
                        .take(10)
                        .forEachIndexed { entryIndex, entry ->

                            OutText(
                                value = entry,
                                label = "Entry ${entryIndex + 1}",
                                onValueChange = { value ->

                                    val updated =
                                        placeholder.entries
                                            .mapIndexed { itemIndex, item ->

                                                if (itemIndex == entryIndex) {
                                                    value
                                                } else {
                                                    item
                                                }
                                            }

                                    onChange(
                                        placeholder.copy(
                                            entries = updated
                                        )
                                    )
                                }
                            )
                        }

                    if (placeholder.entries.size < 10) {

                        TextButton(
                            onClick = {

                                onChange(
                                    placeholder.copy(
                                        entries =
                                            placeholder.entries + ""
                                    )
                                )
                            }
                        ) {
                            Text("+ Add entry")
                        }
                    }
                }

                Text(
                    text = "Font size: ${placeholder.fontSizeSp.toInt()} sp"
                )

                Slider(
                    value = placeholder.fontSizeSp,
                    onValueChange = { value ->

                        onChange(
                            placeholder.copy(
                                fontSizeSp = value
                            )
                        )
                    },
                    valueRange = 12f..48f
                )
            }
        }
    }
}

private fun previewText(
    placeholder: TextPlaceholder
): String {

    val text =
        placeholder.entries.firstOrNull()
            ?: ""

    return when (placeholder.type) {

        PlaceholderType.TEXT ->
            text

        PlaceholderType.BULLETS ->
            text.lines().joinToString("\n") { line ->
                "• $line"
            }

        PlaceholderType.CHECKLIST ->
            text.lines().joinToString("\n") { line ->
                "☐ $line"
            }

        PlaceholderType.TABLE ->
            text
    }
}

@Composable
private fun OutText(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Content"
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HistoryTab(
    items: List<WallpaperConfig>,
    onUse: (WallpaperConfig) -> Unit
) {

    if (items.isEmpty()) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No history yet")
        }

    } else {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(items) { config ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "${config.placeholders.size} placeholders",
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                onUse(config)
                            }
                        ) {
                            Text("Use")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    config: WallpaperConfig,
    onChange: (WallpaperConfig) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Text("Dynamic wallpaper refresh interval")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            listOf(15, 30, 60).forEach { minutes ->

                FilterChip(
                    selected =
                        config.dynamicIntervalMinutes == minutes,

                    onClick = {

                        onChange(
                            config.copy(
                                dynamicIntervalMinutes = minutes
                            )
                        )
                    },

                    label = {
                        Text("$minutes min")
                    }
                )
            }
        }

        Text(
            "Dynamic mode supports up to 10 entries per placeholder."
        )
    }
}

private fun applyWallpaper(
    context: Context,
    config: WallpaperConfig
) {

    val wallpaperManager =
        WallpaperManager.getInstance(context)

    val baseBitmap: Bitmap? =
        if (config.wallpaperUri != null) {

            context.contentResolver
                .openInputStream(
                    Uri.parse(config.wallpaperUri)
                )
                ?.use { input ->
                    BitmapFactory.decodeStream(input)
                }

        } else {
            null
        }

    val bitmap =
        baseBitmap
            ?: Bitmap.createBitmap(
                1080,
                2400,
                Bitmap.Config.ARGB_8888
            ).also { createdBitmap ->

                createdBitmap.eraseColor(
                    config.wallpaperColor.toInt()
                )
            }

    val rendered =
        WallpaperRenderer.render(
            bitmap,
            config
        )

    val output =
        ByteArrayOutputStream()

    rendered.compress(
        Bitmap.CompressFormat.PNG,
        100,
        output
    )

    wallpaperManager.setStream(
        output.toByteArray().inputStream(),
        null,
        true,
        WallpaperManager.FLAG_LOCK
    )

    val hasDynamic =
        config.placeholders.any { placeholder ->
            placeholder.mode == DisplayMode.DYNAMIC
        }

    val workManager =
        WorkManager.getInstance(context)

    if (hasDynamic) {

        val request =
            PeriodicWorkRequestBuilder<
                com.walltext.app.wallpaper.DynamicWallpaperWorker
            >(
                config.dynamicIntervalMinutes.toLong(),
                TimeUnit.MINUTES
            ).build()

        workManager.enqueueUniquePeriodicWork(
            "dynamic_wallpaper",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

    } else {

        workManager.cancelUniqueWork(
            "dynamic_wallpaper"
        )
    }
}