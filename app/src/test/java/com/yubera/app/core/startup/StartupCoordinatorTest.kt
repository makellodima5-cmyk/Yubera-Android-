package com.yubera.app.core.startup

import com.yubera.app.core.session.SessionStateProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupCoordinatorTest {

    @Test
    fun initializeOpensLocalStorageBeforeRoutingHomeForRestorableSession() = runTest {
        val events = mutableListOf<String>()
        val storage = FakeLocalStorageInitializer(events)
        val sessionProvider = FakeSessionStateProvider(hasSession = true, events = events)
        val coordinator = StartupCoordinator(storage, sessionProvider)

        val destination = coordinator.initialize()

        assertEquals(StartupDestination.Home, destination)
        assertEquals(listOf("storage", "session"), events)
        assertEquals(1, sessionProvider.readCount)
    }

    @Test
    fun initializeOpensLocalStorageBeforeRoutingToSignInWithoutSession() = runTest {
        val events = mutableListOf<String>()
        val storage = FakeLocalStorageInitializer(events)
        val sessionProvider = FakeSessionStateProvider(hasSession = false, events = events)
        val coordinator = StartupCoordinator(storage, sessionProvider)

        val destination = coordinator.initialize()

        assertEquals(StartupDestination.SignIn, destination)
        assertEquals(listOf("storage", "session"), events)
        assertEquals(1, sessionProvider.readCount)
    }

    private class FakeLocalStorageInitializer(
        private val events: MutableList<String>
    ) : StartupLocalStorageInitializer {
        override suspend fun initialize() {
            events += "storage"
        }
    }

    private class FakeSessionStateProvider(
        private val hasSession: Boolean,
        private val events: MutableList<String>
    ) : SessionStateProvider {
        var readCount: Int = 0
            private set

        override suspend fun hasRestorableSession(): Boolean {
            events += "session"
            readCount += 1
            return hasSession
        }
    }
}
