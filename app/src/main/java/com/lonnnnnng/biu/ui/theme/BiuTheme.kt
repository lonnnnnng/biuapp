package com.lonnnnnng.biu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val darkColors = darkColorScheme(
    primary = Color(0xFF3DDC84),
    onPrimary = Color(0xFF00210F),
    primaryContainer = Color(0xFF123D25),
    onPrimaryContainer = Color(0xFFA2F4C3),
    secondary = Color(0xFFB8C9BF),
    onSecondary = Color(0xFF24342B),
    secondaryContainer = Color(0xFF33443A),
    onSecondaryContainer = Color(0xFFD4E8DB),
    tertiary = Color(0xFFA7C8B2),
    background = Color(0xFF0D0F0E),
    surface = Color(0xFF121513),
    surfaceVariant = Color(0xFF303532),
    surfaceContainer = Color(0xFF181C19),
    surfaceContainerLow = Color(0xFF141714),
    surfaceContainerHigh = Color(0xFF202420),
    onBackground = Color(0xFFE4EAE6),
    onSurface = Color(0xFFE4EAE6),
    onSurfaceVariant = Color(0xFFAEB8B1),
    outline = Color(0xFF78827B),
    outlineVariant = Color(0xFF303632),
    error = Color(0xFFFFB4AB),
)

private val biuTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
)

private val biuShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
)

@Composable
fun BiuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColors,
        typography = biuTypography,
        shapes = biuShapes,
        content = content,
    )
}
