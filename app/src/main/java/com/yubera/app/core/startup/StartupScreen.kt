package com.yubera.app.core.startup

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yubera.app.R
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaPrimary
import kotlinx.coroutines.delay

@Composable
fun StartupScreen(state: StartupUiState, onRetry: () -> Unit, onReady: () -> Unit) {
    val shownAt = remember { SystemClock.elapsedRealtime() }
    LaunchedEffect(state) {
        if (state is StartupUiState.Ready) {
            val remaining = 3_000L - (SystemClock.elapsedRealtime() - shownAt)
            if (remaining > 0) delay(remaining)
            onReady()
        }
    }
    val transition = rememberInfiniteTransition(label = "splash")
    val wave = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5_500, easing = LinearEasing)), label = "wave")
    Box(Modifier.fillMaxSize().background(YuberaBackground)) {
        Image(painterResource(R.drawable.yubera_splash_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, YuberaBackground.copy(.22f), YuberaBackground.copy(.5f), Color.Transparent))))
        SplashWaves(wave.value)
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.yubera_logo_vertical), "YUBERA", Modifier.width(190.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(28.dp))
            when (state) {
                StartupUiState.Initializing, is StartupUiState.Ready -> CircularProgressIndicator(Modifier.size(42.dp), color = YuberaPrimary, trackColor = YuberaMuted.copy(alpha = .3f), strokeWidth = 2.8.dp)
                is StartupUiState.Failed -> {
                    Text("Не удалось подготовить приложение", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry, enabled = state.retryable, colors = ButtonDefaults.buttonColors(containerColor = YuberaPrimary)) { Text("Повторить") }
                }
            }
        }
    }
}

@Composable
private fun SplashWaves(phase: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w=size.width; val h=size.height; val y=h*.78f
        val curves=listOf(
            Path().apply { moveTo(-w*.08f,y+h*.04f); cubicTo(w*.22f,y-h*.12f,w*.43f,y+h*.12f,w*.66f,y+h*.02f); cubicTo(w*.82f,y-h*.05f,w*1.04f,y+h*.02f,w*1.12f,y+h*.05f) },
            Path().apply { moveTo(-w*.06f,y+h*.15f); cubicTo(w*.18f,y+h*.07f,w*.36f,y+h*.19f,w*.54f,y+h*.14f); cubicTo(w*.75f,y+h*.09f,w*.91f,y+h*.17f,w*1.08f,y+h*.13f) }
        )
        curves.forEachIndexed { i,p ->
            drawPath(p, Color(0xFF8E351D).copy(alpha=.44f), style=Stroke(1.2.dp.toPx(), cap=StrokeCap.Round))
            val x=((phase+i*.39f)%1f)*w
            drawPath(p, Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFF6A20).copy(.08f), Color(0xFFFF8A36).copy(.7f), Color(0xFFFFB15B), Color(0xFFFF6A20).copy(.35f), Color.Transparent), startX=x-w*.17f,endX=x+w*.17f), style=Stroke(2.2.dp.toPx(), cap=StrokeCap.Round))
        }
    }
}
