package com.yubera.app

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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

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
                            StartupDestination.Home -> AppNavHost()
                        }
                    }
                }
            }
        }
    }
}
