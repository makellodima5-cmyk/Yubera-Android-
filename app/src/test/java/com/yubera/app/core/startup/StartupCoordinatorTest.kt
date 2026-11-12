package com.yubera.app.core.startup

import com.yubera.app.core.session.SessionStateProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupCoordinatorTest {

    @Test
    fun initializeRoutesToHomeWhenSessionCanBeRestored() = runTest {
        val sessionProvider = FakeSessionStateProvider(hasSession = true)
        val coordinator = StartupCoordinator(sessionProvider)

        val destination = coordinator.initialize()

        assertEquals(StartupDestination.Home, destination)
        assertEquals(1, sessionProvider.readCount)
    }

    @Test
    fun initializeRoutesToSignInWhenNoSessionCanBeRestored() = runTest {
        val sessionProvider = FakeSessionStateProvider(hasSession = false)
        val coordinator = StartupCoordinator(sessionProvider)

        val destination = coordinator.initialize()

        assertEquals(StartupDestination.SignIn, destination)
        assertEquals(1, sessionProvider.readCount)
    }

    private class FakeSessionStateProvider(
        private val hasSession: Boolean
    ) : SessionStateProvider {
        var readCount: Int = 0
            private set

        override suspend fun hasRestorableSession(): Boolean {
            readCount += 1
            return hasSession
        }
    }
}
