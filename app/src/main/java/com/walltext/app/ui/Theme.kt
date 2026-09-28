package com.walltext.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val WallTextColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,

    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,

    background = Color.Black,
    onBackground = Color.White,

    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,

    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFB8B8BD),

    outline = Color(0xFF3A3A3C),

    error = Color(0xFFFF453A),
    onError = Color.White
)

private val WallTextTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 42.sp,
        fontWeight = FontWeight.Bold
    ),

    displayMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold
    ),

    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold
    ),

    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    ),

    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    ),

    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),

    titleMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold
    ),

    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp
    ),

    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp
    )
)

@Composable
fun Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WallTextColors,
        typography = WallTextTypography,
        content = content
    )
}