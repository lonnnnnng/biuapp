package com.lonnnnnng.biu.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.lonnnnnng.biu.data.local.AppListDensity
import com.lonnnnnng.biu.data.local.AppTextScale
import com.lonnnnnng.biu.data.local.AppThemeMode

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

private val lightColors = lightColorScheme(
    primary = Color(0xFF006D3B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA2F4C3),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4F6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8D8),
    onSecondaryContainer = Color(0xFF0C1F15),
    tertiary = Color(0xFF3D6650),
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDDE5DF),
    surfaceContainer = Color(0xFFEDF2EE),
    surfaceContainerLow = Color(0xFFF3F7F4),
    surfaceContainerHigh = Color(0xFFE7ECE8),
    onBackground = Color(0xFF181C19),
    onSurface = Color(0xFF181C19),
    onSurfaceVariant = Color(0xFF414943),
    outline = Color(0xFF717973),
    outlineVariant = Color(0xFFC1C9C3),
    error = Color(0xFFBA1A1A),
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

data class BiuListDensityMetrics(
    val contentVerticalPadding: Dp,
    val rowVerticalPadding: Dp,
    val mediaThumbnailWidth: Dp,
    val mediaThumbnailHeight: Dp,
    val mediaRowSpacing: Dp,
    val localAudioArtworkSize: Dp,
    val dynamicThumbnailWidth: Dp,
    val dynamicRowSpacing: Dp,
)

private val standardListDensityMetrics = BiuListDensityMetrics(
    contentVerticalPadding = 4.dp,
    rowVerticalPadding = 4.dp,
    mediaThumbnailWidth = 88.dp,
    mediaThumbnailHeight = 52.dp,
    mediaRowSpacing = 10.dp,
    localAudioArtworkSize = 48.dp,
    dynamicThumbnailWidth = 112.dp,
    dynamicRowSpacing = 8.dp,
)

val LocalBiuListDensity = staticCompositionLocalOf { standardListDensityMetrics }

private fun AppListDensity.toMetrics(): BiuListDensityMetrics = when (this) {
    AppListDensity.STANDARD -> standardListDensityMetrics
    AppListDensity.COMPACT -> BiuListDensityMetrics(
        contentVerticalPadding = 1.dp,
        rowVerticalPadding = 2.dp,
        mediaThumbnailWidth = 76.dp,
        mediaThumbnailHeight = 45.dp,
        mediaRowSpacing = 8.dp,
        localAudioArtworkSize = 44.dp,
        dynamicThumbnailWidth = 96.dp,
        dynamicRowSpacing = 6.dp,
    )
}

private fun TextStyle.scaledBy(multiplier: Float): TextStyle = copy(
    fontSize = fontSize * multiplier,
    lineHeight = lineHeight * multiplier,
)

private fun Typography.scaledBy(multiplier: Float): Typography = copy(
    displayLarge = displayLarge.scaledBy(multiplier),
    displayMedium = displayMedium.scaledBy(multiplier),
    displaySmall = displaySmall.scaledBy(multiplier),
    headlineLarge = headlineLarge.scaledBy(multiplier),
    headlineMedium = headlineMedium.scaledBy(multiplier),
    headlineSmall = headlineSmall.scaledBy(multiplier),
    titleLarge = titleLarge.scaledBy(multiplier),
    titleMedium = titleMedium.scaledBy(multiplier),
    titleSmall = titleSmall.scaledBy(multiplier),
    bodyLarge = bodyLarge.scaledBy(multiplier),
    bodyMedium = bodyMedium.scaledBy(multiplier),
    bodySmall = bodySmall.scaledBy(multiplier),
    labelLarge = labelLarge.scaledBy(multiplier),
    labelMedium = labelMedium.scaledBy(multiplier),
    labelSmall = labelSmall.scaledBy(multiplier),
)

@Composable
fun BiuTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    textScale: AppTextScale = AppTextScale.STANDARD,
    listDensity: AppListDensity = AppListDensity.STANDARD,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colorScheme = if (useDarkTheme) darkColors else lightColors
    val systemBarColor = colorScheme.background.toArgb()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // long: 系统栏必须与当前主题同时切换背景和图标明暗，避免深色内容配亮色状态栏或反向低对比。
            (view.context as? ComponentActivity)?.enableEdgeToEdge(
                statusBarStyle = if (useDarkTheme) {
                    SystemBarStyle.dark(systemBarColor)
                } else {
                    SystemBarStyle.light(systemBarColor, systemBarColor)
                },
                navigationBarStyle = if (useDarkTheme) {
                    SystemBarStyle.dark(systemBarColor)
                } else {
                    SystemBarStyle.light(systemBarColor, systemBarColor)
                },
            )
            (view.context as? ComponentActivity)?.makeGestureNavigationTransparent()
        }
    }
    // long: 应用字号只放大 Compose 基准排版，系统无障碍字号仍由 sp 的系统缩放继续生效。
    val typography = biuTypography.scaledBy(textScale.multiplier)
    CompositionLocalProvider(LocalBiuListDensity provides listDensity.toMetrics()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = biuShapes,
        ) {
            // long: 根 Surface 延伸到透明系统栏下方，Android 15+ 强制 edge-to-edge 时也不会露出窗口默认底色。
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                content = content,
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun ComponentActivity.makeGestureNavigationTransparent() {
    // long: 底部 TabBar 会延伸到手势区，导航栏必须透明，否则系统颜色层会盖住贴底标签。
    window.navigationBarColor = Color.Transparent.toArgb()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
