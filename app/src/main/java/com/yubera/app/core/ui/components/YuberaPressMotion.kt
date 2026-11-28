package com.yubera.app.core.ui.components

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberYuberaPressInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

@Composable
fun Modifier.yuberaPressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.975f): Modifier {
    val context = LocalContext.current
    val motionAllowed = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
            .getOrDefault(true)
    }
    val pressed by interactionSource.collectIsPressedAsState()
    val target = if (pressed && motionAllowed) pressedScale else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (pressed) 140 else 180),
        label = "yuberaPressScale"
    )
    return this.scale(scale)
}
