package com.yubera.app.core.startup

sealed interface StartupUiState {
    data object Initializing : StartupUiState
    data class Ready(val destination: StartupDestination) : StartupUiState
    data class Failed(val retryable: Boolean) : StartupUiState
}
