package com.yubera.app.core.startup

import android.os.SystemClock
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yubera.app.R
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaPrimary
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun StartupScreen(state:StartupUiState,onRetry:()->Unit,onReady:()->Unit) {
    val shownAt=remember { SystemClock.elapsedRealtime() }
    LaunchedEffect(state) {
        if(state is StartupUiState.Ready) {
            val remaining=3_000L-(SystemClock.elapsedRealtime()-shownAt)
            if(remaining>0) delay(remaining)
            onReady()
        }
    }
    val context= LocalContext.current
    val reduceMotion=remember {
        runCatching { Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f }.getOrDefault(false)
    }
    val transition= rememberInfiniteTransition(label="splash")
    val wave by transition.animateFloat(
        initialValue=0f,targetValue=1f,
        animationSpec=infiniteRepeatable(tween(7_000,easing=LinearEasing)),
        label="wave"
    )
    val spinner by transition.animateFloat(
        initialValue=0f,targetValue=360f,
        animationSpec=infiniteRepeatable(tween(1_050,easing=LinearEasing)),
        label="spinner"
    )
    Box(Modifier.fillMaxSize().background(YuberaBackground)) {
        Image(painterResource(R.drawable.yubera_splash_background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent,YuberaBackground.copy(.12f),YuberaBackground.copy(.28f),Color.Transparent))))
        if(!reduceMotion) SplashWaves(wave)
        Column(
            Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top=heightFraction(.49f)),
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Image(painterResource(R.drawable.yubera_logo_vertical),"YUBERA",Modifier.width(190.dp),contentScale=ContentScale.Fit)
            Spacer(Modifier.height(30.dp))
            when(state) {
                StartupUiState.Initializing,is StartupUiState.Ready -> {
                    if(reduceMotion) CircularProgressIndicator(Modifier.size(42.dp),color=YuberaPrimary,trackColor=YuberaMuted.copy(.3f),strokeWidth=2.8.dp)
                    else Canvas(Modifier.size(42.dp)) {
                        drawCircle(YuberaMuted.copy(.34f),style=Stroke(width=2.8.dp.toPx()))
                        drawArc(YuberaPrimary,startAngle=spinner,sweepAngle=102f,useCenter=false,style=Stroke(width=2.8.dp.toPx(),cap=StrokeCap.Round))
                    }
                }
                is StartupUiState.Failed -> {
                    Text("Не удалось подготовить приложение",color=MaterialTheme.colorScheme.onBackground,fontSize=14.sp,fontWeight=FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick=onRetry,enabled=state.retryable,colors=ButtonDefaults.buttonColors(containerColor=YuberaPrimary)){Text("Повторить")}
                }
            }
        }
    }
}

@Composable
private fun heightFraction(fraction:Float):androidx.compose.ui.unit.Dp {
    val density=androidx.compose.ui.platform.LocalDensity.current
    val config=androidx.compose.ui.platform.LocalConfiguration.current
    return with(density){(config.screenHeightDp.dp.toPx()*fraction).toDp()}
}

@Composable
private fun SplashWaves(phase:Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w=size.width; val h=size.height; val y=h*.78f
        val paths=listOf(
            Path().apply{moveTo(-w*.08f,y+h*.04f);cubicTo(w*.22f,y-h*.12f,w*.43f,y+h*.12f,w*.66f,y+h*.02f);cubicTo(w*.82f,y-h*.05f,w*1.04f,y+h*.02f,w*1.12f,y+h*.05f)},
            Path().apply{moveTo(-w*.06f,y+h*.15f);cubicTo(w*.18f,y+h*.07f,w*.36f,y+h*.19f,w*.54f,y+h*.14f);cubicTo(w*.75f,y+h*.09f,w*.91f,y+h*.17f,w*1.08f,y+h*.13f)}
        )
        paths.forEachIndexed { index,path ->
            drawPath(path,Color(0xFF8E351D).copy(.48f),style=Stroke(1.4.dp.toPx(),cap=StrokeCap.Round))
            val center=((phase+index*.37f)%1f)*w
            val points=21
            for(step in 0 until points) {
                val position=step/(points-1f)
                val sample=center-w*.22f+position*w*.44f
                val strength=(1f-abs(position-.5f)*2f).coerceAtLeast(0f)
                val pathGradient=Brush.horizontalGradient(
                    colors=listOf(Color.Transparent,Color(0xFFFF5A1F).copy(alpha=.05f),Color(0xFFFFA04E).copy(alpha=.7f*strength),Color(0xFFFFC06A).copy(alpha=.92f*strength),Color.Transparent),
                    startX=sample-w*.035f,endX=sample+w*.035f
                )
                drawPath(path,pathGradient,style=Stroke(width=(1.4f+2.2f*strength).dp.toPx(),cap=StrokeCap.Round))
            }
        }
    }
}
