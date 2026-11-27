package com.yubera.app.core.ui.theme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
internal data class YuberaPalette(val background:Color,val surface:Color,val surfaceElevated:Color,val primary:Color,val accent:Color,val onBackground:Color,val muted:Color)
internal val LocalYuberaPalette=staticCompositionLocalOf { YuberaPalette(Color(0xFF111216),Color(0xFF1A1B20),Color(0xFF202126),Color(0xFFF47724),Color(0xFFFF9A3D),Color(0xFFF2F2F3),Color(0xFFA7A7AD)) }
val YuberaBackground:Color @Composable get()=LocalYuberaPalette.current.background
val YuberaSurface:Color @Composable get()=LocalYuberaPalette.current.surface
val YuberaSurfaceElevated:Color @Composable get()=LocalYuberaPalette.current.surfaceElevated
val YuberaPrimary:Color @Composable get()=LocalYuberaPalette.current.primary
val YuberaAccent:Color @Composable get()=LocalYuberaPalette.current.accent
val YuberaOnBackground:Color @Composable get()=LocalYuberaPalette.current.onBackground
val YuberaMuted:Color @Composable get()=LocalYuberaPalette.current.muted
