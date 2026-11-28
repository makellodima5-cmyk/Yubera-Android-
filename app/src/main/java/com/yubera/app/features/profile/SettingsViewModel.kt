package com.yubera.app.features.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.yubera.app.core.session.NativeAuthRepository\nimport com.yubera.app.core.session.ActiveAccountStore\nimport com.yubera.app.core.database.dao.AnimeEpisodeNotificationDao
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.data.auth.YuberaProfileApi
import com.yubera.app.data.auth.YuberaCommunityResponse
import com.yubera.app.data.auth.TelegramSessionUser
import com.yubera.app.data.settings.ThemeMode
import com.yubera.app.data.settings.UserSettings
import com.yubera.app.data.settings.UserSettingsRepository
import com.yubera.app.features.updates.AppUpdateCheckResult
import com.yubera.app.features.updates.AppUpdateCoordinator
import com.yubera.app.features.updates.AppUpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.flatMapLatest\nimport kotlinx.coroutines.flow.flowOf\nimport kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountProfileUiState(val user: TelegramSessionUser? = null, val isLoading: Boolean = false, val errorMessage: String? = null)
data class AppUpdateUiState(val isChecking: Boolean = false, val checkResult: AppUpdateCheckResult? = null, val availableUpdate: AppUpdateInfo? = null, val isMandatory: Boolean = false, val downloadWorkInfo: WorkInfo? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserSettingsRepository,
    private val authRepository: NativeAuthRepository,
    private val appUpdateCoordinator: AppUpdateCoordinator,
    private val yuberaProfileApi: YuberaProfileApi,
    private val catalogRepository: CatalogRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val settings = repository.settings.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), UserSettings())
    private val mutableAccountProfile = MutableStateFlow(AccountProfileUiState())
    val accountProfile = mutableAccountProfile.asStateFlow()
    private val mutableCommunityData = MutableStateFlow<YuberaCommunityResponse?>(null)
    val communityData = mutableCommunityData.asStateFlow()
    val catalog = catalogRepository.observeCatalog().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())\n    val unreadNotificationCount = activeAccountStore.activeAccountId.flatMapLatest { accountId -> if (accountId.isNullOrBlank()) flowOf(0) else notificationDao.observeUnreadDeliveredCount(accountId) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    private val profileCache = context.getSharedPreferences("yubera_profile_cache", Context.MODE_PRIVATE)
    private val profileJson = Json { ignoreUnknownKeys = true }
    private val mutableAppUpdate = MutableStateFlow(AppUpdateUiState(availableUpdate = appUpdateCoordinator.storedUpdate(), isMandatory = appUpdateCoordinator.storedMandatory()))
    val appUpdate = mutableAppUpdate.asStateFlow()

    init {
        viewModelScope.launch { runCatching { catalogRepository.refreshIfStale() } }
        viewModelScope.launch { appUpdateCoordinator.observeDownload().collect { work -> mutableAppUpdate.update { it.copy(downloadWorkInfo = work) } } }
    }

    fun checkForUpdates() {
        if (mutableAppUpdate.value.isChecking) return
        viewModelScope.launch {
            mutableAppUpdate.update { it.copy(isChecking = true) }
            val result = appUpdateCoordinator.check()
            mutableAppUpdate.update { state -> state.copy(
                isChecking = false, checkResult = result,
                availableUpdate = when (result) { is AppUpdateCheckResult.Available -> result.update; AppUpdateCheckResult.UpToDate -> null; is AppUpdateCheckResult.Failed -> state.availableUpdate },
                isMandatory = when (result) { is AppUpdateCheckResult.Available -> result.isMandatory; AppUpdateCheckResult.UpToDate -> false; is AppUpdateCheckResult.Failed -> state.isMandatory }
            ) }
        }
    }

    fun downloadUpdate() { mutableAppUpdate.value.availableUpdate?.let(appUpdateCoordinator::download) }
    fun createInstallIntent(): android.content.Intent? {
        val work = mutableAppUpdate.value.downloadWorkInfo ?: return null
        val update = mutableAppUpdate.value.availableUpdate ?: return null
        return appUpdateCoordinator.createUserConfirmedInstallIntent(work, update)
    }

    fun refreshAccountProfile() {
        if (mutableAccountProfile.value.isLoading) return
        viewModelScope.launch {
            mutableAccountProfile.value = AccountProfileUiState(isLoading = true)
            val authorization = authRepository.getSessionAuthorization()
            val storedAccountId = authRepository.getStoredAccountId()
            if (storedAccountId != null) {
                profileCache.getString("community_${storedAccountId}", null)?.let { cached ->
                    runCatching { profileJson.decodeFromString<YuberaCommunityResponse>(cached) }
                        .getOrNull()?.let { mutableCommunityData.value = it }
                }
            }
            if (authorization == null || storedAccountId == null) {
                mutableAccountProfile.value = AccountProfileUiState(errorMessage = "Профиль аккаунта временно недоступен.")
                return@launch
            }
            val user = runCatching { authRepository.getCurrentProfile() }.getOrNull()
            if (user == null || user.id != storedAccountId) {
                mutableAccountProfile.value = AccountProfileUiState(errorMessage = "Не удалось обновить аккаунт. Показаны сохранённые данные, если они доступны.")
                return@launch
            }
            try {
                val result = yuberaProfileApi.getMyProfile(authorization)
                if (!result.authenticated || result.profile == null) {
                    mutableAccountProfile.value = AccountProfileUiState(user = user, errorMessage = "Профиль YUBERA недоступен. Показаны сохранённые данные.")
                    return@launch
                }
                mutableAccountProfile.value = AccountProfileUiState(user = user)
                mutableCommunityData.value = result
                profileCache.edit().putString("community_${user.id}", profileJson.encodeToString(result)).apply()
            } catch (_: Exception) {
                mutableAccountProfile.value = AccountProfileUiState(user = user, errorMessage = "Не удалось обновить профиль. Показаны сохранённые данные.")
            }
        }
    }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { repository.setThemeMode(mode) } }
    fun setNotifications(enabled: Boolean) { viewModelScope.launch { repository.setNotificationsEnabled(enabled) } }
    fun setNewEpisodes(enabled: Boolean) { viewModelScope.launch { repository.setNewEpisodeNotificationsEnabled(enabled) } }
    fun setAnimeInfo(enabled: Boolean) { viewModelScope.launch { repository.setAnimeInfoNotificationsEnabled(enabled) } }
    fun setPushNotifications(enabled: Boolean) { viewModelScope.launch { repository.setPushNotificationsEnabled(enabled) } }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try { authRepository.logout() } catch (_: Exception) { }
            finally {
                mutableAccountProfile.value = AccountProfileUiState()
                mutableCommunityData.value = null
                onComplete()
            }
        }
    }
}
