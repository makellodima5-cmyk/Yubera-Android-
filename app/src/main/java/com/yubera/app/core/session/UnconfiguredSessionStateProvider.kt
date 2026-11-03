package com.yubera.app.core.session

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Temporary explicit default until the native authentication flow is connected.
 *
 * This implementation deliberately reports no restorable session. It is not
 * an authentication implementation and must be replaced when the existing
 * YUBERA identity/backend flow has been inspected and the native session is
 * implemented.
 */
@Singleton
class UnconfiguredSessionStateProvider @Inject constructor() : SessionStateProvider {
    override suspend fun hasRestorableSession(): Boolean = false
}
