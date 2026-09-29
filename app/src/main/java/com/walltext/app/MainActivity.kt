package com.walltext.app

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.heightIn
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
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
val savedConfig by store.current.collectAsState(
    initial = null
)

LaunchedEffect(savedConfig) {
    savedConfig?.let { saved ->
        config = saved
    }
}
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
        containerColor = Color.Black,

        topBar = {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(Color.Black)
            .padding(
                start = 18.dp,
                end = 18.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(1.dp)
            ) {

                Text(
                    text = when (selectedTab) {
                        0 -> "WallText"
                        1 -> "Editor"
                        2 -> "History"
                        else -> "Settings"
                    },

                    style =
                        MaterialTheme.typography
                            .headlineSmall,

                    color = Color.White
                )

                Text(
                    text = when (selectedTab) {
                        0 -> "Your phone. Your words."
                        1 -> "Make it yours."
                        2 -> "Your saved designs."
                        else -> "Control WallText."
                    },

                    style =
                        MaterialTheme.typography
                            .bodySmall,

                    color = Color(0xFF77777C)
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        Color(0xFF1C1C1E),
                        RoundedCornerShape(50.dp)
                    )
                    .clickable {

                        scope.launch {

                            store.save(config)

                            applyWallpaper(
                                context,
                                config
                            )
                        }
                    }
                    .padding(
                        horizontal = 20.dp,
                        vertical = 11.dp
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "Apply",
                    color = Color.White,
                    style =
                        MaterialTheme.typography
                            .labelLarge
                )
            }
        }
    }
},

        bottomBar = {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(
                start = 18.dp,
                end = 18.dp,
                bottom = 12.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(
                    Color(0xFF1C1C1E),
                    RoundedCornerShape(35.dp)
                )
                .padding(6.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            val tabs = listOf(
                "⌂" to "Home",
                "✦" to "Edit",
                "◷" to "History",
                "⚙" to "Settings"
            )

            tabs.forEachIndexed { index, item ->

                val selected =
                    selectedTab == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(
                            if (selected) {
                                Color(0xFF2C2C2E)
                            } else {
                                Color.Transparent
                            },
                            RoundedCornerShape(30.dp)
                        )
                        .clickable {
                            selectedTab = index
                        },

                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.spacedBy(1.dp)
                    ) {

                        Text(
                            text = item.first,

                            fontSize = 22.sp,

                            color =
                                if (selected) {
                                    Color.White
                                } else {
                                    Color(0xFF77777C)
                                }
                        )

                        Text(
                            text = item.second,

                            fontSize = 11.sp,

                            color =
                                if (selected) {
                                    Color.White
                                } else {
                                    Color(0xFF77777C)
                                }
                        )
                    }
                }
            }
        }
    }
},

    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
        ) {

            when (selectedTab) {

                0 -> {

                    WallpaperTab(
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
                }

                1 -> {

                    EditorTab(
                        config = config,

                        onChange = { newConfig ->
                            config = newConfig
                        }
                    )
                }

                2 -> {

                    HistoryTab(
                        items = history,

                        onUse = { oldConfig ->
                            config = oldConfig
                        }
                    )
                }

                3 -> {

                    SettingsTab(
                        config = config,

                        onChange = { newConfig ->
                            config = newConfig
                        }
                    )
                }
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
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 10.dp,
                bottom = 24.dp
            ),

        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // Large hero heading
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "Your wallpaper",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Text(
                text = "Your phone. Your words.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF9E9EA3)
            )
        }

        // Large wallpaper preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(470.dp),

            shape = RoundedCornerShape(30.dp)
        ) {

            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                if (config.wallpaperUri != null) {

                    AsyncImage(
                        model = config.wallpaperUri,

                        contentDescription =
                            "Selected wallpaper",

                        modifier = Modifier.fillMaxSize(),

                        contentScale = ContentScale.Crop
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(
                                    config.wallpaperColor
                                )
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            Text(
                                text = "＋",
                                fontSize = 42.sp,
                                color = Color.White
                            )

                            Text(
                                text = "Add a wallpaper",
                                style =
                                    MaterialTheme.typography
                                        .titleLarge,

                                color = Color.White
                            )

                            Text(
                                text =
                                    "Choose a photo from your phone",
                                color =
                                    Color.White.copy(
                                        alpha = 0.6f
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Primary action
        Button(
            onClick = onPick,

            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),

            shape = RoundedCornerShape(30.dp)
        ) {

            Text(
                text = "＋  Add wallpaper",
                fontSize = 17.sp
            )
        }

        // Secondary actions
        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            OutlinedButton(
                onClick = {

                    onChange(
                        config.copy(
                            wallpaperUri = null,
                            wallpaperColor =
                                0xFF101114
                        )
                    )
                },

                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),

                shape = RoundedCornerShape(27.dp)
            ) {

                Text("Solid")
            }

            OutlinedButton(
                onClick = {
                    // Editing is handled from the Edit tab.
                },

                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),

                shape = RoundedCornerShape(27.dp)
            ) {

                Text("Edit text")
            }
        }

        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f),

                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = "Wallpaper status",

                        style =
                            MaterialTheme.typography
                                .titleMedium,

                        color = Color.White
                    )

                    Text(
                        text =
                            if (
                                config.wallpaperUri != null
                            ) {
                                "Photo selected"
                            } else {
                                "No photo selected"
                            },

                        color =
                            Color(0xFF9E9EA3)
                    )
                }

                Text(
                    text =
                        if (
                            config.wallpaperUri != null
                        ) {
                            "●"
                        } else {
                            "○"
                        },

                    fontSize = 22.sp,

                    color = Color.White
                )
            }
        }

        // Placeholder summary
        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp),

                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "Text holders",

                    style =
                        MaterialTheme.typography
                            .titleMedium,

                    color = Color.White
                )

                Text(
                    text =
                        "${config.placeholders.size} of 5 placeholders",

                    color =
                        Color(0xFF9E9EA3)
                )

                Text(
                    text =
                        "Add or position your text from Edit.",

                    color =
                        Color(0xFF77777C),

                    style =
                        MaterialTheme.typography
                            .bodyMedium
                )
            }
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
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 10.dp,
                bottom = 24.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        // Header
        Column(
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "Edit wallpaper",
                style =
                    MaterialTheme.typography
                        .headlineLarge,
                color = Color.White
            )

            Text(
                text = "Place your words exactly where you want them.",
                style =
                    MaterialTheme.typography.bodyLarge,
                color = Color(0xFF9E9EA3)
            )
        }

        // Live preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),

            shape =
                RoundedCornerShape(30.dp)
        ) {

            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                if (config.wallpaperUri != null) {

                    AsyncImage(
                        model = config.wallpaperUri,

                        contentDescription =
                            "Wallpaper preview",

                        modifier =
                            Modifier.fillMaxSize(),

                        contentScale =
                            ContentScale.Crop
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(
                                    config.wallpaperColor
                                )
                            )
                    )
                }

                // Subtle dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = 0.18f
                            )
                        )
                )

                // Draggable placeholders
BoxWithConstraints(
    modifier = Modifier.fillMaxSize()
) {

    val density = LocalDensity.current

    config.placeholders
        .forEachIndexed { index, placeholder ->

            val holderWidth =
                maxWidth * placeholder.width

            val xOffset =
                maxWidth *
                    (
                        placeholder.x -
                            placeholder.width / 2f
                    )

            val yOffset =
                maxHeight * placeholder.y

            Box(
                modifier =
                    Modifier
                        .width(holderWidth)
                        .heightIn(min = 64.dp)
                        .offset(
                            x = xOffset,
                            y = yOffset
                        )
                        .pointerInput(
                            placeholder.id
                        ) {

                            detectDragGestures {

                                change,
                                dragAmount ->

                                change.consume()

                                val previewWidthPx =
                                    with(density) {
                                        maxWidth.toPx()
                                    }

                                val previewHeightPx =
                                    with(density) {
                                        maxHeight.toPx()
                                    }

                                if (
                                    previewWidthPx <= 0f ||
                                    previewHeightPx <= 0f
                                ) {
                                    return@detectDragGestures
                                }

                                val newX =
                                    (
                                        placeholder.x +
                                            dragAmount.x /
                                            previewWidthPx
                                    ).coerceIn(
                                        placeholder.width / 2f,
                                        1f -
                                            placeholder.width /
                                            2f
                                    )

                                val newY =
                                    (
                                        placeholder.y +
                                            dragAmount.y /
                                            previewHeightPx
                                    ).coerceIn(
                                        0f,
                                        1f
                                    )

                                val updated =
                                    config
                                        .placeholders
                                        .mapIndexed {
                                            itemIndex,
                                            item ->

                                            if (
                                                itemIndex == index
                                            ) {

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
                                        placeholders =
                                            updated
                                    )
                                )
                            }
                        }
            ) {

                Text(
                    text = previewText(placeholder),
                    color = Color.White.copy(
                        alpha = placeholder.opacity
                    ),
                    fontSize = placeholder.fontSizeSp.sp
                )
            }
        }
}

                if (
                    config.placeholders.isEmpty()
                ) {

                    Column(
                        modifier =
                            Modifier.align(
                                Alignment.Center
                            ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text = "＋",
                            fontSize = 40.sp,
                            color = Color.White
                        )

                        Text(
                            text = "Add your first text holder",
                            style =
                                MaterialTheme.typography
                                    .titleLarge,
                            color = Color.White
                        )

                        Text(
                            text =
                                "Then drag it anywhere on the wallpaper.",
                            color =
                                Color.White.copy(
                                    alpha = 0.65f
                                )
                        )
                    }
                }
            }
        }

        // Preview hint
        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(22.dp)
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f),

                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = "Live preview",

                        style =
                            MaterialTheme.typography
                                .titleMedium,

                        color = Color.White
                    )

                    Text(
                        text =
                            "Press and drag text to reposition it.",

                        color =
                            Color(0xFF9E9EA3)
                    )
                }

                Text(
                    text =
                        "${config.placeholders.size}/5",

                    style =
                        MaterialTheme.typography
                            .titleMedium,

                    color = Color.White
                )
            }
        }

        // Placeholder list
        Text(
            text = "Text holders",

            style =
                MaterialTheme.typography
                    .headlineSmall,

            color = Color.White
        )

        config.placeholders.forEachIndexed {
                index,
                placeholder ->

            PlaceholderCard(

                index = index,

                placeholder = placeholder,

                onChange = { newPlaceholder ->

                    val updated =
                        config.placeholders
                            .mapIndexed {
                                    itemIndex,
                                    item ->

                                if (
                                    itemIndex == index
                                ) {
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
                        config.placeholders
                            .filterIndexed {
                                    itemIndex,
                                    _ ->

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

        // Add placeholder
        if (
            config.placeholders.size < 5
        ) {

            Button(

                onClick = {

                    val newPlaceholder =
                        TextPlaceholder(

                            id = UUID
                                .randomUUID()
                                .toString(),

                            title =
                                "Placeholder ${
                                    config.placeholders.size + 1
                                }"
                        )

                    onChange(
                        config.copy(
                            placeholders =
                                config.placeholders +
                                    newPlaceholder
                        )
                    )
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),

                shape =
                    RoundedCornerShape(30.dp)
            ) {

                Text(
                    text =
                        "＋  Add text holder",

                    fontSize = 17.sp
                )
            }
        }

        if (
            config.placeholders.size >= 5
        ) {

            Text(
                text =
                    "Maximum of 5 text holders reached.",

                modifier =
                    Modifier.fillMaxWidth(),

                color =
                    Color(0xFF77777C),

                textAlign =
                    androidx.compose.ui.text.style
                        .TextAlign.Center
            )
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

    val typeLabel = when (placeholder.type) {
        PlaceholderType.TEXT -> "Text"
        PlaceholderType.TABLE -> "Table"
        PlaceholderType.CHECKLIST -> "Checklist"
        PlaceholderType.BULLETS -> "Bullets"
    }

    val modeLabel = when (placeholder.mode) {
        DisplayMode.STATIC -> "Static"
        DisplayMode.DYNAMIC -> "Dynamic"
    }

    val icon = when (placeholder.type) {
        PlaceholderType.TEXT -> "T"
        PlaceholderType.TABLE -> "▦"
        PlaceholderType.CHECKLIST -> "☑"
        PlaceholderType.BULLETS -> "•"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF2C2C2E),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = icon,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {

                    Text(
                        text = placeholder.title,
                        style =
                            MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Text(
                        text = "$typeLabel  •  $modeLabel",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E9EA3)
                    )
                }

                TextButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {

                    Text(
                        text =
                            if (expanded) {
                                "Done"
                            } else {
                                "Edit"
                            }
                    )
                }
            }

            if (expanded) {

                // Name
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
                        Text("Holder name")
                    },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()
                )

                // Type
                Text(
                    text = "Holder type",
                    style =
                        MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    PlaceholderType
                        .values()
                        .forEach { type ->

                            FilterChip(
                                selected =
                                    placeholder.type == type,

                                onClick = {

                                    onChange(
                                        placeholder.copy(
                                            type = type
                                        )
                                    )
                                },

                                label = {

                                    Text(
                                        when (type) {
                                            PlaceholderType.TEXT ->
                                                "Text"

                                            PlaceholderType.TABLE ->
                                                "Table"

                                            PlaceholderType.CHECKLIST ->
                                                "Checklist"

                                            PlaceholderType.BULLETS ->
                                                "Bullets"
                                        }
                                    )
                                }
                            )
                        }
                }

                // Display mode
                Text(
                    text = "Display mode",
                    style =
                        MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    FilterChip(
                        selected =
                            placeholder.mode ==
                                DisplayMode.STATIC,

                        onClick = {

                            onChange(
                                placeholder.copy(
                                    mode =
                                        DisplayMode.STATIC
                                )
                            )
                        },

                        label = {
                            Text("Static")
                        }
                    )

                    FilterChip(
                        selected =
                            placeholder.mode ==
                                DisplayMode.DYNAMIC,

                        onClick = {

                            onChange(
                                placeholder.copy(
                                    mode =
                                        DisplayMode.DYNAMIC
                                )
                            )
                        },

                        label = {
                            Text("Dynamic")
                        }
                    )
                }

                // Content
                if (
                    placeholder.mode ==
                    DisplayMode.STATIC
                ) {

                    Text(
                        text = "Content",
                        style =
                            MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    OutText(
                        value =
                            placeholder.entries
                                .firstOrNull()
                                ?: "",

                        onValueChange = { value ->

                            onChange(
                                placeholder.copy(
                                    entries =
                                        listOf(value)
                                )
                            )
                        }
                    )

                } else {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    "Dynamic entries",

                                style =
                                    MaterialTheme.typography
                                        .titleMedium,

                                color = Color.White
                            )

                            Text(
                                text =
                                    "${placeholder.entries.size}/10 entries",

                                color =
                                    Color(0xFF9E9EA3)
                            )
                        }

                        TextButton(

                            onClick = {

                                if (
                                    placeholder.entries.size < 10
                                ) {

                                    onChange(
                                        placeholder.copy(
                                            entries =
                                                placeholder.entries +
                                                    ""
                                        )
                                    )
                                }
                            }
                        ) {

                            Text("+ Add")
                        }
                    }

                    placeholder.entries
                        .take(10)
                        .forEachIndexed {
                                entryIndex,
                                entry ->

                            OutText(
                                value = entry,

                                label =
                                    "Entry ${
                                        entryIndex + 1
                                    }",

                                onValueChange = {
                                        value ->

                                    val updated =
                                        placeholder.entries
                                            .mapIndexed {
                                                    itemIndex,
                                                    item ->

                                                if (
                                                    itemIndex ==
                                                    entryIndex
                                                ) {
                                                    value
                                                } else {
                                                    item
                                                }
                                            }

                                    onChange(
                                        placeholder.copy(
                                            entries =
                                                updated
                                        )
                                    )
                                }
                            )
                        }
                }

                // Font size
                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(14.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text =
                                "Font size  ${
                                    placeholder.fontSizeSp
                                        .toInt()
                                } sp",

                            style =
                                MaterialTheme.typography
                                    .titleMedium,

                            color = Color.White
                        )

                        Slider(

                            value =
                                placeholder.fontSizeSp,

                            onValueChange = { value ->

                                onChange(
                                    placeholder.copy(
                                        fontSizeSp =
                                            value
                                    )
                                )
                            },

                            valueRange =
                                12f..48f
                        )
                    }
                }

                // Delete
                OutlinedButton(

                    onClick = onDelete,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),

                    shape =
                        RoundedCornerShape(25.dp)
                ) {

                    Text(
                        text = "Delete holder",
                        color = Color(0xFFFF453A)
                    )
                }
            }
        }
    }
}

private fun previewText(
    placeholder: TextPlaceholder
): String {

    val text =
        placeholder.entries
            .firstOrNull()
            ?: ""

    return when (
        placeholder.type
    ) {

        PlaceholderType.TEXT ->
            text

        PlaceholderType.BULLETS ->

            text.lines()
                .joinToString("\n") { line ->
                    "• $line"
                }

        PlaceholderType.CHECKLIST ->

            text.lines()
                .joinToString("\n") { line ->
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 10.dp,
                bottom = 24.dp
            )
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "History",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Text(
                text = "Your last 10 wallpaper setups.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF9E9EA3)
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        if (items.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "◷",
                        fontSize = 38.sp,
                        color = Color.White
                    )

                    Text(
                        text = "No history yet",
                        style =
                            MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )

                    Text(
                        text =
                            "Your saved wallpaper configurations will appear here.",
                        color = Color(0xFF9E9EA3)
                    )
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                items(items) { config ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFF2C2C2E),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp)
                            ) {

                                Text(
                                    text = "▣",
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(14.dp)
                            )

                            Column(
                                modifier =
                                    Modifier.weight(1f),

                                verticalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {

                                Text(
                                    text =
                                        if (
                                            config.wallpaperUri != null
                                        ) {
                                            "Photo wallpaper"
                                        } else {
                                            "Solid wallpaper"
                                        },

                                    style =
                                        MaterialTheme.typography
                                            .titleMedium,

                                    color = Color.White
                                )

                                Text(
                                    text =
                                        "${config.placeholders.size} text holder${
                                            if (
                                                config.placeholders.size != 1
                                            ) "s" else ""
                                        }",

                                    color =
                                        Color(0xFF9E9EA3)
                                )
                            }

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
}

@Composable
private fun SettingsTab(
    config: WallpaperConfig,
    onChange: (WallpaperConfig) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 10.dp,
                bottom = 24.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "Settings",
                style =
                    MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Text(
                text =
                    "Control how WallText behaves.",
                style =
                    MaterialTheme.typography.bodyLarge,
                color = Color(0xFF9E9EA3)
            )
        }

        // Dynamic wallpaper
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF2C2C2E),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {

                        Text(
                            text = "↻",
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f),

                        verticalArrangement =
                            Arrangement.spacedBy(3.dp)
                    ) {

                        Text(
                            text =
                                "Dynamic wallpaper",

                            style =
                                MaterialTheme.typography
                                    .titleMedium,

                            color = Color.White
                        )

                        Text(
                            text =
                                "Change displayed entries automatically.",

                            color =
                                Color(0xFF9E9EA3)
                        )
                    }
                }

                Text(
                    text =
                        "Refresh interval",

                    style =
                        MaterialTheme.typography
                            .titleMedium,

                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    listOf(
                        15,
                        30,
                        60
                    ).forEach { minutes ->

                        FilterChip(

                            selected =
                                config.dynamicIntervalMinutes ==
                                    minutes,

                            onClick = {

                                onChange(
                                    config.copy(
                                        dynamicIntervalMinutes =
                                            minutes
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
                    text =
                        "Android controls the exact execution time of background work. The selected interval is the requested minimum period.",

                    style =
                        MaterialTheme.typography
                            .bodySmall,

                    color =
                        Color(0xFF77777C)
                )
            }
        }

        // Text holder limits
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Text holders",
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    color = Color.White
                )

                Text(
                    text =
                        "Up to 5 holders per wallpaper.",

                    color =
                        Color(0xFF9E9EA3)
                )

                Text(
                    text =
                        "Each dynamic holder can contain up to 10 entries.",

                    color =
                        Color(0xFF9E9EA3)
                )
            }
        }

        // Wallpaper mode
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Wallpaper",
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    color = Color.White
                )

                Text(
                    text =
                        if (
                            config.wallpaperUri != null
                        ) {
                            "Using a photo from your device."
                        } else {
                            "Using a solid background."
                        },

                    color =
                        Color(0xFF9E9EA3)
                )
            }
        }

        // About
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp)
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "WallText",
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    color = Color.White
                )

                Text(
                    text = "Personal wallpapers with your own words.",

                    color =
                        Color(0xFF9E9EA3)
                )

                Text(
                    text = "Version 1.0",

                    color =
                        Color(0xFF77777C)
                )
            }
        }
    }
}
private fun applyWallpaper(
    context: Context,
    config: WallpaperConfig
) {

    val wallpaperManager =
        WallpaperManager.getInstance(
            context
        )

    val baseBitmap: Bitmap? =

        if (
            config.wallpaperUri != null
        ) {

            context.contentResolver

                .openInputStream(
                    Uri.parse(
                        config.wallpaperUri
                    )
                )

                ?.use { input ->

                    BitmapFactory
                        .decodeStream(input)
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

    val textScale =
    context.resources.displayMetrics.scaledDensity *
        (bitmap.width / 1080f)

val rendered =
    WallpaperRenderer.render(
        base = bitmap,
        config = config,
        textScale = textScale
    )

    val output =
        ByteArrayOutputStream()

    rendered.compress(
        Bitmap.CompressFormat.PNG,
        100,
        output
    )

    wallpaperManager.setStream(

        output
            .toByteArray()
            .inputStream(),

        null,

        true,

        WallpaperManager.FLAG_LOCK
    )

    val hasDynamic =
        config.placeholders.any { placeholder ->

            placeholder.mode ==
                DisplayMode.DYNAMIC
        }

    val workManager =
        WorkManager.getInstance(
            context
        )

    if (hasDynamic) {

        val request =
            PeriodicWorkRequestBuilder<
                com.walltext.app.wallpaper
                    .DynamicWallpaperWorker
            >(
                config.dynamicIntervalMinutes
                    .toLong(),

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