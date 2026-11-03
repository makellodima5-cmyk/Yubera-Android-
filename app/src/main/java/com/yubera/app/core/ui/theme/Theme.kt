package com.yubera.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.yubera.app.data.settings.ThemeMode

private val DarkPalette = YuberaPalette(
    background = Color(0xFF101116),
    surface = Color(0xFF1A1B22),
    surfaceElevated = Color(0xFF22232B),
    primary = Color(0xFFFF5A3D),
    accent = Color(0xFFFF764F),
    onBackground = Color(0xFFF6F4F5),
    muted = Color(0xFFA4A1AA)
)

private val LightPalette = YuberaPalette(
    background = Color(0xFFF7F5F6),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFECE9EC),
    primary = Color(0xFFD9472D),
    accent = Color(0xFFB94C38),
    onBackground = Color(0xFF1B1920),
    muted = Color(0xFF6C6872)
)

@Composable
fun YuberaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val palette = when (themeMode) {
        ThemeMode.SYSTEM -> if (systemDark) DarkPalette else LightPalette
        ThemeMode.LIGHT -> LightPalette
        ThemeMode.DARK -> DarkPalette
    }
    val colorScheme = if (themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && systemDark)) {
        darkColorScheme(
            primary = palette.primary,
            secondary = palette.accent,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.onBackground,
            onSurface = palette.onBackground
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.accent,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.onBackground,
            onSurface = palette.onBackground
        )
    }
    CompositionLocalProvider(LocalYuberaPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
