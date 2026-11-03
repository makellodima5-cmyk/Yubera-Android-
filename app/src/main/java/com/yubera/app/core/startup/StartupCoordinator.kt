package com.yubera.app.core.startup

import com.yubera.app.core.session.SessionStateProvider
import javax.inject.Inject

/**
 * Resolves the first app destination from the currently available session state.
 *
 * Authentication-specific behavior belongs in SessionStateProvider, not here.
 */
class StartupCoordinator @Inject constructor(
    private val sessionStateProvider: SessionStateProvider
) {
    suspend fun initialize(): StartupDestination =
        if (sessionStateProvider.hasRestorableSession()) {
            StartupDestination.Home
        } else {
            StartupDestination.SignIn
        }
}
