package com.yubera.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    private var pendingEpisodeRoute: PendingEpisodeRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingEpisodeRoute = intent.toPendingEpisodeRoute()

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            YuberaTheme(themeMode = settings.themeMode) {
                val startupState by startupViewModel.uiState.collectAsState()
                when (val state = startupState) {
                    StartupUiState.Initializing,
                    is StartupUiState.Failed -> StartupScreen(
                        state = state,
                        onRetry = startupViewModel::retry
                    )
                    is StartupUiState.Ready -> {
                        when (state.destination) {
                            StartupDestination.SignIn -> AuthNavHost()
                            StartupDestination.Home -> AppNavHost(
                                pendingEpisodeRoute = pendingEpisodeRoute,
                                onEpisodeRouteHandled = { pendingEpisodeRoute = null }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingEpisodeRoute = intent.toPendingEpisodeRoute()
    }

    companion object {
        const val ACTION_OPEN_EPISODE = "com.yubera.app.action.OPEN_EPISODE"
        const val EXTRA_ANIME_ID = "com.yubera.app.extra.ANIME_ID"
        const val EXTRA_EPISODE_NUMBER = "com.yubera.app.extra.EPISODE_NUMBER"
    }
}

data class PendingEpisodeRoute(
    val animeId: String,
    val episodeNumber: Int
)

private fun Intent.toPendingEpisodeRoute(): PendingEpisodeRoute? {
    if (action != MainActivity.ACTION_OPEN_EPISODE) return null
    val animeId = getStringExtra(MainActivity.EXTRA_ANIME_ID)?.takeIf { it.isNotBlank() } ?: return null
    if (!animeId.matches(Regex("\\d{1,12}")) || animeId.toLongOrNull()?.let { it > 0 } != true) return null
    val episode = getIntExtra(MainActivity.EXTRA_EPISODE_NUMBER, -1)
    if (episode <= 0) return null
    return PendingEpisodeRoute(animeId, episode)
}
