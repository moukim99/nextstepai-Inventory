package com.nextstepai.inventory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nextstepai.inventory.data.AppThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF3525CD),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4F46E5),
    onPrimaryContainer = Color(0xFFDAD7FF),
    inversePrimary = Color(0xFFC3C0FF),
    secondary = Color(0xFF0058BE),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2170E4),
    onSecondaryContainer = Color(0xFFFEFCFF),
    tertiary = Color(0xFF005338),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF006E4B),
    onTertiaryContainer = Color(0xFF67F4B7),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF0B1C30),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF0B1C30),
    surfaceVariant = Color(0xFFD3E4FE),
    onSurfaceVariant = Color(0xFF464555),
    inverseSurface = Color(0xFF213145),
    inverseOnSurface = Color(0xFFEAF1FF),
    outline = Color(0xFF777587),
    outlineVariant = Color(0xFFC7C4D8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC3C0FF),
    onPrimary = Color(0xFF1E00A5),
    primaryContainer = Color(0xFF3525CD),
    onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFFADC6FF),
    onSecondary = Color(0xFF002E69),
    secondaryContainer = Color(0xFF004395),
    onSecondaryContainer = Color(0xFFD8E2FF),
    tertiary = Color(0xFF6FFBBE),
    onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF005236),
    onTertiaryContainer = Color(0xFF8DF8DA),
    background = Color(0xFF0B1C30),
    onBackground = Color(0xFFEAF1FF),
    surface = Color(0xFF0B1C30),
    onSurface = Color(0xFFEAF1FF),
    surfaceVariant = Color(0xFF464555),
    onSurfaceVariant = Color(0xFFC7C4D8),
    inverseSurface = Color(0xFFEAF1FF),
    inverseOnSurface = Color(0xFF0B1C30),
    outline = Color(0xFF918F9F),
    outlineVariant = Color(0xFF464555),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/**
 * مظهر التطبيق الشامل (AppTheme) المعتمد على MaterialTheme دون أي ألوان ملونة ثابته داخل المكونات.
 */
@Composable
fun AppTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
