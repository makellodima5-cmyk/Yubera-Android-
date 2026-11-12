package com.yubera.app.core.startup

import com.yubera.app.core.session.SessionStateProvider
import javax.inject.Inject

/**
 * Prepares local storage before resolving the first app destination.
 * Authentication-specific behavior belongs in SessionStateProvider.
 */
class StartupCoordinator @Inject constructor(
    private val localStorageInitializer: StartupLocalStorageInitializer,
    private val sessionStateProvider: SessionStateProvider
) {
    suspend fun initialize(): StartupDestination {
        localStorageInitializer.initialize()
        return if (sessionStateProvider.hasRestorableSession()) {
            StartupDestination.Home
        } else {
            StartupDestination.SignIn
        }
    }
}
