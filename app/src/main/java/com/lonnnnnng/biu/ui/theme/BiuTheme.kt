package com.lonnnnnng.biu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val lightColors = lightColorScheme(
    primary = Color(0xFFD93F68),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF6D1530),
    secondary = Color(0xFF51565F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E6EA),
    onSecondaryContainer = Color(0xFF20242A),
    tertiary = Color(0xFF59618D),
    background = Color(0xFFF7F7F4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECECE8),
    surfaceContainer = Color(0xFFF0F0ED),
    surfaceContainerLow = Color(0xFFF8F8F5),
    surfaceContainerHigh = Color(0xFFE8E8E4),
    onBackground = Color(0xFF1D1D1F),
    onSurface = Color(0xFF1D1D1F),
    onSurfaceVariant = Color(0xFF5F6064),
    outline = Color(0xFF797A7E),
    outlineVariant = Color(0xFFD1D1CD),
    error = Color(0xFFBA1A1A),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFFFB1C2),
    onPrimary = Color(0xFF7D1736),
    primaryContainer = Color(0xFF9B2B4B),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFC7C9D1),
    onSecondary = Color(0xFF2C3036),
    secondaryContainer = Color(0xFF3C414A),
    onSecondaryContainer = Color(0xFFE4E6ED),
    tertiary = Color(0xFFC1C6FF),
    background = Color(0xFF151517),
    surface = Color(0xFF1C1C1F),
    surfaceVariant = Color(0xFF45464A),
    surfaceContainer = Color(0xFF242427),
    surfaceContainerLow = Color(0xFF202023),
    surfaceContainerHigh = Color(0xFF2F2F32),
    onBackground = Color(0xFFF0F0ED),
    onSurface = Color(0xFFF0F0ED),
    onSurfaceVariant = Color(0xFFC7C7C3),
    outline = Color(0xFF929398),
    outlineVariant = Color(0xFF45464A),
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
        colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors,
        typography = biuTypography,
        shapes = biuShapes,
        content = content,
    )
}
