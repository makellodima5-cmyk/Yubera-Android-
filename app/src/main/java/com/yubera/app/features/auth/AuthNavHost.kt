package com.yubera.app.features.auth

import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
    NavHost(navController=nav,startDestination=AuthRoute.Welcome,modifier=Modifier.fillMaxSize(),
        enterTransition={fadeIn(tween(220))+slideInHorizontally(tween(220)){it/12}},
        exitTransition={fadeOut(tween(180))+slideOutHorizontally(tween(180)){-it/24}},
        popEnterTransition={fadeIn(tween(220))+slideInHorizontally(tween(220)){-it/12}},
        popExitTransition={fadeOut(tween(180))+slideOutHorizontally(tween(180)){it/24}}) {
        composable(AuthRoute.Welcome){WelcomeScreen({nav.navigate(AuthRoute.SignIn){launchSingleTop=true}},{nav.navigate(AuthRoute.Register){launchSingleTop=true}})}
        composable(AuthRoute.SignIn){AuthSignInScreen(vm,{nav.popBackStack()},{nav.navigate(AuthRoute.Register){launchSingleTop=true}})}
        composable(AuthRoute.Register){RegistrationScreen(vm){nav.navigate(AuthRoute.SignIn){launchSingleTop=true}}}
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
                Text("Твой мир аниме\nв одном приложении",color=Color(0xFFD0CDD4),fontSize=17.sp,lineHeight=23.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(bottom=22.dp))
                WelcomeButton("Войти",Icons.Default.Login,true,onSignIn)
                WelcomeButton("Регистрация",Icons.Default.PersonAdd,false,onRegister,Modifier.padding(top=12.dp))
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
@Composable private fun WelcomeButton(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector,primary:Boolean,onClick:()->Unit,modifier:Modifier=Modifier) {
    val shape=RoundedCornerShape(22.dp); val interaction=remember{MutableInteractionSource()}; val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).975f else 1f,tween(if(pressed)150 else 190),label="welcomePress")
    Box(modifier.fillMaxWidth().height(58.dp).scale(scale).background(if(primary)Brush.horizontalGradient(listOf(Color(0xFFFF7A12),Color(0xFFFF4E17))) else Brush.linearGradient(listOf(Color(0xC015141C),Color(0xC015141C))),shape).border(1.dp,if(primary)Color(0xFFFFA15B).copy(.7f) else Color(0xFF77727F),shape).clickable(interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick).padding(horizontal=18.dp),contentAlignment=Alignment.Center) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) { Icon(icon,null,tint=Color.White,modifier=Modifier.size(21.dp)); Spacer(Modifier.width(10.dp)); Text(text,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.SemiBold) }
    }
}

@Composable private fun AuthSignInScreen(vm:AuthViewModel,onBack:()->Unit,onRegister:()->Unit) {
    val context=LocalContext.current
    var launching by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(YuberaBackground)) {
        Image(painterResource(R.drawable.yubera_login_background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops=arrayOf(0f to Color.Transparent,.36f to Color.Transparent,.58f to YuberaBackground.copy(.56f),.78f to YuberaBackground.copy(.90f),1f to YuberaBackground))))
        Column(Modifier.fillMaxSize().padding(WindowInsets.safeDrawing.asPaddingValues()).padding(horizontal=24.dp,vertical=20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Image(painterResource(R.drawable.yubera_logo_vertical),"Фирменный знак и название YUBERA",Modifier.width(140.dp),contentScale=ContentScale.Fit)
            Spacer(Modifier.weight(1f))
            Text("Вход",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            TelegramActionButton(text="Войти через Telegram",enabled=!launching) {
                if(launching) return@TelegramActionButton
                launching=true
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(vm.createAuthorizationUrl("login")))) }
                    .onFailure { launching=false }
            }
            RegistrationLink(onRegister,Modifier.padding(top=10.dp))
            Spacer(Modifier.weight(1f,fill=true))
        }
    }
}
@Composable private fun TelegramSignInButton(onClick:()->Unit) {
    TelegramActionButton(text="Войти через Telegram",onClick=onClick)
}
@Composable private fun TelegramActionButton(text:String,enabled:Boolean=true,onClick:()->Unit) {
    val interaction=remember{MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).975f else 1f,tween(if(pressed)150 else 190),label="telegramActionPress")
    Row(Modifier.fillMaxWidth().height(58.dp).scale(scale).background(Color(0xFF229ED9),RoundedCornerShape(18.dp)).clickable(interactionSource=interaction,indication=null,enabled=enabled,role=Role.Button,onClick=onClick).padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
        TelegramPlane(Modifier.size(22.dp))
        Spacer(Modifier.width(11.dp))
        Text(text,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun TelegramPlane(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val p=Path().apply{moveTo(size.width*.04f,size.height*.43f);lineTo(size.width*.94f,size.height*.06f);lineTo(size.width*.73f,size.height*.94f);lineTo(size.width*.49f,size.height*.65f);lineTo(size.width*.32f,size.height*.79f);lineTo(size.width*.35f,size.height*.55f);close()}
        drawPath(p,Color.White)
        val fold=Path().apply{moveTo(size.width*.35f,size.height*.55f);lineTo(size.width*.94f,size.height*.06f);lineTo(size.width*.49f,size.height*.65f);close()}
        drawPath(fold,Color(0xFFB9E6FA))
    }
}
@Composable private fun RegistrationLink(onClick:()->Unit,modifier:Modifier=Modifier,prefix:String="Или ",linkText:String="зарегистрироваться") {
    val interaction=remember{MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    Row(modifier.fillMaxWidth().heightIn(min=48.dp).clickable(interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
        Text(prefix,color=YuberaMuted,fontSize=15.sp)
        Text(linkText,color=if(pressed) Color(0xFFFFB16E) else Color(0xFFFF7A12),fontSize=15.sp,textDecoration=TextDecoration.Underline,fontWeight=FontWeight.Medium)
    }
}

@Composable private fun RegistrationScreen(vm:AuthViewModel,onSignIn:()->Unit) {
    val context=LocalContext.current
    var launching by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(YuberaBackground)) {
        Image(painterResource(R.drawable.yubera_login_background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops=arrayOf(0f to Color.Transparent,.38f to Color.Transparent,.58f to YuberaBackground.copy(.46f),.78f to YuberaBackground.copy(.88f),1f to YuberaBackground.copy(.92f)))))
        Column(Modifier.fillMaxSize().padding(WindowInsets.safeDrawing.asPaddingValues()).padding(horizontal=24.dp,vertical=20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Image(painterResource(R.drawable.yubera_logo_vertical),"Фирменный знак и название YUBERA",Modifier.width(140.dp),contentScale=ContentScale.Fit)
            Spacer(Modifier.weight(1f))
            Text("Регистрация",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            TelegramActionButton(text="Зарегистрироваться",enabled=!launching) {
                if(launching) return@TelegramActionButton
                launching=true
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(vm.createAuthorizationUrl("register")))) }
                    .onFailure { launching=false }
            }
            RegistrationLink(onSignIn,Modifier.padding(top=10.dp),prefix="Или ",linkText="войти")
            Spacer(Modifier.weight(1f,fill=true))
        }
    }
}
