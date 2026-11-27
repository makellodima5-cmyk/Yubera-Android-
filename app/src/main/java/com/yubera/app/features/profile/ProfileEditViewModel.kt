package com.yubera.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.session.NativeAuthRepository
import com.yubera.app.data.auth.YuberaProfileApi
import com.yubera.app.data.auth.YuberaCommunityProfile
import com.yubera.app.data.auth.YuberaProfileUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileEditState(
    val profile: YuberaCommunityProfile? = null,
    val bio: String = "",
    val visibility: String = "private",
    val selectedFrame: String? = null,
    val category: String = "support",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val api: YuberaProfileApi,
    private val auth: NativeAuthRepository
) : ViewModel() {
    private val mutable = MutableStateFlow(ProfileEditState())
    val state = mutable.asStateFlow()

    fun load() {
        if (!mutable.value.loading) return
        viewModelScope.launch {
            val authorization = auth.getSessionAuthorization()
            if (authorization == null) {
                mutable.value = mutable.value.copy(loading = false, error = "Войдите в аккаунт YUBERA.")
                return@launch
            }
            try {
                val result = api.getMyProfile(authorization)
                val p = result.profile
                if (!result.authenticated || p == null) {
                    mutable.value = mutable.value.copy(loading = false, error = "Профиль не найден. Войдите в аккаунт заново.")
                    return@launch
                }
                mutable.value = ProfileEditState(
                    profile = p,
                    bio = p.bio,
                    visibility = p.visibility,
                    selectedFrame = p.avatarFrame,
                    category = if (p.isAdmin) "admin" else "support",
                    loading = false
                )
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(loading = false, error = "Не удалось загрузить профиль. Проверьте соединение.")
            }
        }
    }

    fun setBio(value: String) = mutable.value.let { mutable.value = it.copy(bio = value.take(250)) }
    fun setVisibility(value: String) = mutable.value.let { mutable.value = it.copy(visibility = value) }
    fun setCategory(value: String) = mutable.value.let { mutable.value = it.copy(category = value) }
    fun setFrame(value: String?) = mutable.value.let { mutable.value = it.copy(selectedFrame = value) }

    fun save(onSaved: () -> Unit) {
        val s = mutable.value
        if (s.saving || s.profile == null) return
        viewModelScope.launch {
            mutable.value = s.copy(saving = true, error = null)
            val authorization = auth.getSessionAuthorization()
            if (authorization == null) {
                mutable.value = mutable.value.copy(saving = false, error = "Сессия истекла. Войдите снова.")
                return@launch
            }
            try {
                val result = api.updateProfile(authorization, YuberaProfileUpdateRequest(bio = s.bio, visibility = s.visibility, avatarFrame = s.selectedFrame))
                if (!result.ok) throw IllegalStateException(result.error ?: "Не удалось сохранить профиль.")
                mutable.value = mutable.value.copy(saving = false, saved = true)
                onSaved()
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(saving = false, error = e.message ?: "Ошибка сохранения. Изменения не потеряны.")
            }
        }
    }
}
