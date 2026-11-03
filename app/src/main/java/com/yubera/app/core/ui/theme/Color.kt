package com.yubera.app.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class YuberaPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val accent: Color,
    val onBackground: Color,
    val muted: Color
)

internal val LocalYuberaPalette = staticCompositionLocalOf {
    YuberaPalette(
        background = Color(0xFF101116),
        surface = Color(0xFF1A1B22),
        surfaceElevated = Color(0xFF22232B),
        primary = Color(0xFFFF5A3D),
        accent = Color(0xFFFF764F),
        onBackground = Color(0xFFF6F4F5),
        muted = Color(0xFFA4A1AA)
    )
}

val YuberaBackground: Color @Composable get() = LocalYuberaPalette.current.background
val YuberaSurface: Color @Composable get() = LocalYuberaPalette.current.surface
val YuberaSurfaceElevated: Color @Composable get() = LocalYuberaPalette.current.surfaceElevated
val YuberaPrimary: Color @Composable get() = LocalYuberaPalette.current.primary
val YuberaAccent: Color @Composable get() = LocalYuberaPalette.current.accent
val YuberaOnBackground: Color @Composable get() = LocalYuberaPalette.current.onBackground
val YuberaMuted: Color @Composable get() = LocalYuberaPalette.current.muted
