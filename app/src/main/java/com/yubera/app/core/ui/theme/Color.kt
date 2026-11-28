package com.yubera.app.core.ui.theme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
internal data class YuberaPalette(val background:Color,val surface:Color,val surfaceElevated:Color,val primary:Color,val accent:Color,val onBackground:Color,val muted:Color,val border:Color,val divider:Color,val caption:Color,val disabled:Color)
internal val LocalYuberaPalette=staticCompositionLocalOf { YuberaPalette(Color(0xFF111216),Color(0xFF1A1B20),Color(0xFF202126),Color(0xFFF47724),Color(0xFFFF9A3D),Color(0xFFF2F2F3),Color(0xFFA7A7AD),Color(0xFF35363D),Color(0xFF2B2C32),Color(0xFF8E8E96),Color(0xFF686970)) }
val YuberaBackground:Color @Composable get()=LocalYuberaPalette.current.background
val YuberaSurface:Color @Composable get()=LocalYuberaPalette.current.surface
val YuberaSurfaceElevated:Color @Composable get()=LocalYuberaPalette.current.surfaceElevated
val YuberaPrimary:Color @Composable get()=LocalYuberaPalette.current.primary
val YuberaAccent:Color @Composable get()=LocalYuberaPalette.current.accent
val YuberaOnBackground:Color @Composable get()=LocalYuberaPalette.current.onBackground
val YuberaMuted:Color @Composable get()=LocalYuberaPalette.current.muted
val YuberaBorder:Color @Composable get()=LocalYuberaPalette.current.border
val YuberaDivider:Color @Composable get()=LocalYuberaPalette.current.divider
val YuberaCaption:Color @Composable get()=LocalYuberaPalette.current.caption
val YuberaDisabled:Color @Composable get()=LocalYuberaPalette.current.disabled
val YuberaRed=Color(0xFFFF3B45)
val YuberaGold=Color(0xFFFFB547)
val YuberaTelegramBlue=Color(0xFF229ED9)
val YuberaScrim=Color(0xE8000000)
