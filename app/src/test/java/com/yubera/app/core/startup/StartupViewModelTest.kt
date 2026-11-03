package com.yubera.app.core.startup

import com.yubera.app.core.session.SessionStateProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StartupViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun keepsStartupVisibleUntilInitializationCompletes() = runBlocking {
        val sessionProvider = DeferredSessionStateProvider()
        val viewModel = StartupViewModel(StartupCoordinator(sessionProvider))

        assertEquals(StartupUiState.Initializing, viewModel.uiState.value)
        assertEquals(1, sessionProvider.readCount)

        sessionProvider.result.complete(false)

        assertEquals(
            StartupUiState.Ready(StartupDestination.SignIn),
            viewModel.uiState.value
        )
    }

    @Test
    fun exposesRetryableFailureWhenInitializationThrows() = runBlocking {
        val viewModel = StartupViewModel(
            StartupCoordinator(FailingSessionStateProvider())
        )

        assertEquals(StartupUiState.Failed(retryable = true), viewModel.uiState.value)
    }

    private class DeferredSessionStateProvider : SessionStateProvider {
        val result = CompletableDeferred<Boolean>()
        var readCount: Int = 0
            private set

        override suspend fun hasRestorableSession(): Boolean {
            readCount += 1
            return result.await()
        }
    }

    private class FailingSessionStateProvider : SessionStateProvider {
        override suspend fun hasRestorableSession(): Boolean {
            error("Session storage unavailable")
        }
    }
}
