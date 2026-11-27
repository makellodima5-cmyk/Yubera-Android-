package com.yubera.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yubera.app.core.startup.StartupDestination
import com.yubera.app.core.startup.StartupScreen
import com.yubera.app.core.startup.StartupUiState
import com.yubera.app.core.startup.StartupViewModel
import com.yubera.app.core.ui.theme.YuberaTheme
import com.yubera.app.features.auth.AuthNavHost
import com.yubera.app.features.profile.SettingsViewModel
import com.yubera.app.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startupViewModel: StartupViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var pendingAnimeRoute: PendingAnimeRoute? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingAnimeRoute=intent.toPendingAnimeRoute()
        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            YuberaTheme(themeMode=settings.themeMode) {
                val state by startupViewModel.uiState.collectAsState()
                var splashFinished by remember { mutableStateOf(false) }
                if (!splashFinished) StartupScreen(state, startupViewModel::retry) { splashFinished=true }
                else when(val ready=state) {
                    StartupUiState.Initializing, is StartupUiState.Failed -> StartupScreen(ready, startupViewModel::retry) { splashFinished=false }
                    is StartupUiState.Ready -> when(ready.destination) {
                        StartupDestination.SignIn -> AuthNavHost()
                        StartupDestination.Home -> AppNavHost(pendingAnimeRoute=pendingAnimeRoute, onAnimeRouteHandled={ pendingAnimeRoute=null })
                    }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); pendingAnimeRoute=intent.toPendingAnimeRoute() }
    companion object {
        const val ACTION_OPEN_EPISODE="com.yubera.app.action.OPEN_EPISODE"
        const val EXTRA_ANIME_ID="com.yubera.app.extra.ANIME_ID"
        const val EXTRA_EPISODE_NUMBER="com.yubera.app.extra.EPISODE_NUMBER"
    }
}
data class PendingAnimeRoute(val animeId:String,val episodeNumber:Int?=null)
private fun Intent.toPendingAnimeRoute():PendingAnimeRoute? {
    if(action!=MainActivity.ACTION_OPEN_EPISODE)return null
    val id=getStringExtra(MainActivity.EXTRA_ANIME_ID)?.takeIf{it.isNotBlank()}?:return null
    if(!id.matches(Regex("\\d{1,12}"))||id.toLongOrNull()?.let{it>0}!=true)return null
    return PendingAnimeRoute(id,getIntExtra(MainActivity.EXTRA_EPISODE_NUMBER,-1).takeIf{it>0})
}
