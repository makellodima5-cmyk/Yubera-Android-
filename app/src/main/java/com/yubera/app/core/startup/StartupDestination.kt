package com.yubera.app.core.startup

sealed interface StartupDestination {
    data object SignIn : StartupDestination
    data object Home : StartupDestination
}
