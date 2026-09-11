package com.nextstepai.inventory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nextstepai.inventory.data.AppThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF388E3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = Color(0xFF7B1FA2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF4A148C),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF37474F),
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFB0BEC5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFE8F5E9),
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color(0xFF4A148C),
    tertiaryContainer = Color(0xFF6A1B9A),
    onTertiaryContainer = Color(0xFFF3E5F5),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E12),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF263238),
    onSurfaceVariant = Color(0xFFCFD8DC),
    error = Color(0xFFEF5350),
    onError = Color(0xFF000000),
    outline = Color(0xFF78909C)
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
