package com.yubera.app.core.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val startupCoordinator: StartupCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow<StartupUiState>(StartupUiState.Initializing)
    val uiState: StateFlow<StartupUiState> = _uiState.asStateFlow()

    private var initializationStarted = false

    init {
        initialize()
    }

    fun retry() {
        if (_uiState.value is StartupUiState.Failed) {
            initialize()
        }
    }

    private fun initialize() {
        if (initializationStarted && _uiState.value !is StartupUiState.Failed) return

        initializationStarted = true
        _uiState.value = StartupUiState.Initializing

        viewModelScope.launch {
            try {
                _uiState.value = StartupUiState.Ready(startupCoordinator.initialize())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.value = StartupUiState.Failed(retryable = true)
            }
        }
    }
}
