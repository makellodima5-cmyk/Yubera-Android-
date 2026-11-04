package com.yubera.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.session.NativeAuthRepository
import com.yubera.app.data.auth.TelegramSessionUser
import com.yubera.app.data.settings.ThemeMode
import com.yubera.app.data.settings.UserSettings
import com.yubera.app.data.settings.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountProfileUiState(
    val user: TelegramSessionUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserSettingsRepository,
    private val authRepository: NativeAuthRepository
) : ViewModel() {
    val settings = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings()
    )

    private val mutableAccountProfile = MutableStateFlow(AccountProfileUiState())
    val accountProfile = mutableAccountProfile.asStateFlow()

    fun refreshAccountProfile() {
        if (mutableAccountProfile.value.isLoading) return
        viewModelScope.launch {
            mutableAccountProfile.value = AccountProfileUiState(isLoading = true)
            try {
                val user = authRepository.getCurrentProfile()
                mutableAccountProfile.value = if (user != null) {
                    AccountProfileUiState(user = user)
                } else {
                    AccountProfileUiState(
                        errorMessage = "Не удалось подтвердить активную Telegram-сессию. Войдите в аккаунт заново."
                    )
                }
            } catch (_: Exception) {
                mutableAccountProfile.value = AccountProfileUiState(
                    errorMessage = "Не удалось загрузить данные аккаунта. Проверьте подключение и повторите попытку."
                )
            }
        }
    }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { repository.setThemeMode(mode) } }
    fun setNotifications(enabled: Boolean) { viewModelScope.launch { repository.setNotificationsEnabled(enabled) } }
    fun setNewEpisodes(enabled: Boolean) { viewModelScope.launch { repository.setNewEpisodeNotificationsEnabled(enabled) } }
    fun setAnimeInfo(enabled: Boolean) { viewModelScope.launch { repository.setAnimeInfoNotificationsEnabled(enabled) } }
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (_: Exception) {
                // Local credentials are cleared by NativeAuthRepository even when the request fails.
            } finally {
                mutableAccountProfile.value = AccountProfileUiState()
                onComplete()
            }
        }
    }
}
