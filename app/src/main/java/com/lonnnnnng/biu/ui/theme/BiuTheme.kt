package com.lonnnnnng.biu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColors = lightColorScheme(
    primary = Color(0xFFE24B70),
    onPrimary = Color.White,
    secondary = Color(0xFF147D73),
    background = Color(0xFFFAFAF7),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202124),
    onSurface = Color(0xFF202124),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFFF8EAB),
    secondary = Color(0xFF6FD8CB),
    background = Color(0xFF19191B),
    surface = Color(0xFF242426),
    onBackground = Color(0xFFF1F1EE),
    onSurface = Color(0xFFF1F1EE),
)

@Composable
fun BiuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors,
        content = content,
    )
}
