package com.yubera.app.core.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yubera.app.R
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaPrimary

@Composable
fun StartupScreen(
    state: StartupUiState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(YuberaBackground)
    ) {
        Image(
            painter = painterResource(R.drawable.yubera_splash_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        YuberaBackground.copy(alpha = 0.10f),
                        YuberaBackground.copy(alpha = 0.78f),
                        YuberaBackground
                    )
                )
            )
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            YuberaLogo(width = 224.dp)
            Spacer(modifier = Modifier.height(34.dp))

            when (state) {
                StartupUiState.Initializing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        color = YuberaPrimary,
                        trackColor = YuberaMuted.copy(alpha = 0.35f),
                        strokeWidth = 3.dp
                    )
                }
                is StartupUiState.Failed -> {
                    Text(
                        text = "Не удалось подготовить приложение",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRetry,
                        enabled = state.retryable,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YuberaPrimary)
                    ) {
                        Text("Повторить")
                    }
                }
                is StartupUiState.Ready -> Unit
            }
        }
    }
}
