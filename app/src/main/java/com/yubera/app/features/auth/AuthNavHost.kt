package com.yubera.app.features.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yubera.app.R
import com.yubera.app.core.ui.components.YuberaLogo
import com.yubera.app.core.ui.theme.YuberaBackground
import com.yubera.app.core.ui.theme.YuberaMuted
import com.yubera.app.core.ui.theme.YuberaPrimary

private object AuthRoute { const val Welcome="welcome"; const val SignIn="sign-in"; const val Register="register" }
@Composable fun AuthNavHost() {
    val nav=rememberNavController(); val vm:AuthViewModel=hiltViewModel()
    NavHost(navController=nav,startDestination=AuthRoute.Welcome,modifier=Modifier.fillMaxSize()) {
        composable(AuthRoute.Welcome) { WelcomeScreen({nav.navigate(AuthRoute.SignIn){launchSingleTop=true}},{nav.navigate(AuthRoute.Register){launchSingleTop=true}}) }
        composable(AuthRoute.SignIn) { AuthSignInScreen(vm){nav.popBackStack()} }
        composable(AuthRoute.Register) { AuthRegisterPendingScreen{nav.popBackStack()} }
    }
}
@Composable private fun WelcomeScreen(onSignIn:()->Unit,onRegister:()->Unit) {
    Box(Modifier.fillMaxSize().background(YuberaBackground)) {
        Image(painterResource(R.drawable.yubera_login_background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops=arrayOf(0f to Color.Transparent,.34f to Color.Transparent,.52f to YuberaBackground.copy(.60f),.70f to YuberaBackground.copy(.93f),1f to YuberaBackground.copy(.86f)))))
        Column(Modifier.fillMaxSize().padding(WindowInsets.safeDrawing.asPaddingValues()).padding(horizontal=28.dp,vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween) {
            Spacer(Modifier.height(2.dp))
            Image(painterResource(R.drawable.yubera_logo_vertical),"Фирменный знак и название YUBERA",Modifier.width(156.dp),contentScale=ContentScale.Fit)
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("Твой мир аниме в одном приложении",color=Color(0xFFD0CDD4),fontSize=17.sp,lineHeight=23.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(bottom=22.dp))
                WelcomeButton("Войти",Icons.Default.Login,true,onSignIn)
                WelcomeButton("Регистрация",Icons.Default.PersonAdd,false,onRegister,Modifier.padding(top=12.dp))
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
@Composable private fun WelcomeButton(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector,primary:Boolean,onClick:()->Unit,modifier:Modifier=Modifier) {
    val shape=RoundedCornerShape(22.dp); val interaction=remember{MutableInteractionSource()}; val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).975f else 1f,tween(if(pressed)150 else 190),label="pressScale")
    Box(modifier.fillMaxWidth().height(58.dp).scale(scale).background(if(primary)Brush.horizontalGradient(listOf(Color(0xFFFF7A12),Color(0xFFFF4E17))) else Brush.linearGradient(listOf(Color(0xC015141C),Color(0xC015141C))),shape).border(BorderStroke(1.dp,if(primary)Color(0xFFFFA15B).copy(.7f) else Color(0xFF77727F),shape).clickable(interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick).padding(horizontal=18.dp),contentAlignment=Alignment.Center) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) { Icon(icon,null,tint=Color.White,modifier=Modifier.size(21.dp)); Spacer(Modifier.width(10.dp)); Text(text,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.SemiBold) }
    }
}
@Composable private fun AuthSignInScreen(viewModel:AuthViewModel,onBack:()->Unit) {
    val context=LocalContext.current; var error by remember{mutableStateOf<String?>(null)}
    Box(Modifier.fillMaxSize().background(YuberaBackground)) {
        Image(painterResource(R.drawable.yubera_login_background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,YuberaBackground.copy(.72f),YuberaBackground))))
        Column(Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) {
            YuberaLogo(width=142.dp); Spacer(Modifier.height(24.dp))
            Text("Войти в YUBERA",color=MaterialTheme.colorScheme.onBackground,fontSize=29.sp,fontWeight=FontWeight.Bold)
            Text("Безопасный вход через Telegram. Пароль YUBERA не нужен.",color=YuberaMuted,textAlign=TextAlign.Center,modifier=Modifier.padding(top=10.dp,bottom=24.dp))
            Button(onClick={error=null;try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(viewModel.createAuthorizationUrl("login"))))}catch(e:Exception){error=e.message?: "Не удалось открыть Telegram для входа."}},modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=YuberaPrimary)){Text("Продолжить через Telegram",fontWeight=FontWeight.SemiBold)}
            error?.let{Text(it,color=MaterialTheme.colorScheme.error,textAlign=TextAlign.Center,modifier=Modifier.padding(top=12.dp))}
            OutlinedButton(onBack,Modifier.fillMaxWidth().padding(top=12.dp).height(54.dp),shape=RoundedCornerShape(16.dp)){Text("Назад")}
        }
    }
}
@Composable private fun AuthRegisterPendingScreen(onBack:()->Unit) {
    Box(Modifier.fillMaxSize().background(YuberaBackground)) {
        Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            YuberaLogo(width=142.dp)
            Text("Регистрация временно недоступна",color=MaterialTheme.colorScheme.onBackground,fontSize=25.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,modifier=Modifier.padding(top=28.dp))
            Text("Сначала нужно опубликовать и утвердить юридические документы YUBERA, чтобы регистрация показывала пользователю реальные условия и политику конфиденциальности.",color=YuberaMuted,textAlign=TextAlign.Center,modifier=Modifier.padding(top=12.dp))
            Button(onBack,Modifier.fillMaxWidth().padding(top=28.dp).height(54.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=YuberaPrimary)){Text("Назад")}
        }
    }
}
