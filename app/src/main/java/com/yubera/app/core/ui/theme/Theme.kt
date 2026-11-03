package com.yubera.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val YuberaColors = darkColorScheme(
    primary = YuberaPrimary,
    secondary = YuberaAccent,
    background = YuberaBackground,
    surface = YuberaSurface,
    onBackground = YuberaOnBackground,
    onSurface = YuberaOnBackground
)

@Composable
fun YuberaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YuberaColors,
        content = content
    )
}
