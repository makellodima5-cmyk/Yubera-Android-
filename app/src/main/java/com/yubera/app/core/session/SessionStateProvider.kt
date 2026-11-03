package com.yubera.app.core.session

/**
 * Provides the startup flow with the ability to determine whether a native
 * YUBERA session can be restored without exposing credential storage details.
 */
interface SessionStateProvider {
    suspend fun hasRestorableSession(): Boolean
}
