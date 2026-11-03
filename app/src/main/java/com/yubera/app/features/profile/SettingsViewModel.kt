package com.yubera.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.session.NativeAuthRepository
import com.yubera.app.data.settings.ThemeMode
import com.yubera.app.data.settings.UserSettings
import com.yubera.app.data.settings.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { repository.setThemeMode(mode) } }
    fun setNotifications(enabled: Boolean) { viewModelScope.launch { repository.setNotificationsEnabled(enabled) } }
    fun setNewEpisodes(enabled: Boolean) { viewModelScope.launch { repository.setNewEpisodeNotificationsEnabled(enabled) } }
    fun setAnimeInfo(enabled: Boolean) { viewModelScope.launch { repository.setAnimeInfoNotificationsEnabled(enabled) } }
    fun logout(onComplete: () -> Unit) { viewModelScope.launch { try { authRepository.logout() } catch (_: Exception) { } finally { onComplete() } } }
}
