package com.yubera.app.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yubera.app.R
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaPrimary

private object AuthRoute {
    const val Welcome = "welcome"
    const val SignIn = "sign-in"
    const val Register = "register"
}

@Composable
fun AuthNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AuthRoute.Welcome,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(AuthRoute.Welcome) {
            WelcomeScreen(
                onSignIn = { navController.navigate(AuthRoute.SignIn) },
                onRegister = { navController.navigate(AuthRoute.Register) }
            )
        }
        composable(AuthRoute.SignIn) {
            AuthMethodPlaceholder(
                title = "Войти",
                description = "Вход в YUBERA будет выполняться через Telegram.",
                navController = navController
            )
        }
        composable(AuthRoute.Register) {
            AuthMethodPlaceholder(
                title = "Создать аккаунт",
                description = "Регистрация будет выполняться через Telegram.",
                navController = navController
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    onSignIn: () -> Unit,
    onRegister: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(YuberaBackground)) {
        Image(
            painter = painterResource(R.drawable.yubera_auth_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.40f to YuberaBackground.copy(alpha = 0.12f),
                        0.60f to YuberaBackground.copy(alpha = 0.78f),
                        1.0f to YuberaBackground
                    )
                )
            )
        )
        YuberaLogo(
            modifier = Modifier.align(Alignment.TopStart).padding(start = 26.dp, top = 34.dp),
            width = 142.dp
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(
                start = 24.dp, end = 24.dp, bottom = 28.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Добро пожаловать",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Твой мир аниме начинается здесь.",
                color = YuberaMuted,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = YuberaPrimary)
            ) {
                Text("Войти", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(58.dp),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YuberaMuted)
            ) {
                Text("Создать аккаунт", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "Продолжая, вы принимаете Условия использования и Политику конфиденциальности.",
                color = YuberaMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 22.dp)
            )
        }
    }
}

@Composable
private fun AuthMethodPlaceholder(
    title: String,
    description: String,
    navController: NavHostController
) {
    Box(modifier = Modifier.fillMaxSize().background(YuberaBackground)) {
        Image(
            painter = painterResource(R.drawable.yubera_auth_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, YuberaBackground.copy(alpha = 0.68f), YuberaBackground)
                )
            )
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            YuberaLogo(width = 142.dp)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = YuberaMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp)
                    .border(1.dp, YuberaMuted.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Авторизация будет подключена после проверки совместимости Android-приложения с системой аккаунтов сайта.",
                    color = YuberaMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = YuberaPrimary)
            ) {
                Text("Назад")
            }
        }
    }
}
