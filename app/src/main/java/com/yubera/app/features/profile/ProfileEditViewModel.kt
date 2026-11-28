package com.yubera.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.session.NativeAuthRepository
import com.yubera.app.data.auth.YuberaProfileApi
import com.yubera.app.data.auth.YuberaCommunityProfile
import com.yubera.app.data.auth.YuberaProfileUpdateRequest
import com.yubera.app.core.catalog.CatalogRepository
import com.yubera.app.core.catalog.AnimeCatalogItem
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val selectedAvatarUri: String? = null,
    val selectedAvatarAnimeId: String? = null,
    val customName: String = "",
    val category: String = "support",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val api: YuberaProfileApi,
    private val auth: NativeAuthRepository,
    private val catalogRepository: CatalogRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutable = MutableStateFlow(ProfileEditState())
    val state = mutable.asStateFlow()
    val catalog = catalogRepository.observeCatalog().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load() {
        if (mutable.value.loading && mutable.value.profile != null) return
        mutable.value = mutable.value.copy(loading = true, error = null)
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
                    customName = p.customName?.takeIf { it.isNotBlank() } ?: p.displayName,
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
    fun setCustomName(value: String) = mutable.value.let { mutable.value = it.copy(customName = value.take(20)) }
    fun selectCatalogAvatar(animeId: String) = mutable.value.let { mutable.value = it.copy(selectedAvatarAnimeId = animeId, selectedAvatarUri = null) }
    fun setVisibility(value: String) = mutable.value.let { mutable.value = it.copy(visibility = value) }
    fun setCategory(value: String) = mutable.value.let { mutable.value = it.copy(category = value) }
    fun setFrame(value: String?) = mutable.value.let { mutable.value = it.copy(selectedFrame = value) }
    fun setAvatarUri(value: String?) = mutable.value.let { mutable.value = it.copy(selectedAvatarUri = value, selectedAvatarAnimeId = null) }

    private suspend fun compressAvatar(uri: Uri): String? = withContext(Dispatchers.IO) {
        var source: Bitmap? = null
        var cropped: Bitmap? = null
        var resized: Bitmap? = null
        try {
            val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
            source = decoded
            val side = minOf(decoded.width, decoded.height)
            val x = (decoded.width - side) / 2
            val y = (decoded.height - side) / 2
            val cropBitmap = if (side == decoded.width && side == decoded.height) decoded else Bitmap.createBitmap(decoded, x, y, side, side)
            cropped = cropBitmap
            val scaledBitmap = if (cropBitmap.width == 512 && cropBitmap.height == 512) cropBitmap else Bitmap.createScaledBitmap(cropBitmap, 512, 512, true)
            resized = scaledBitmap
            var quality = 82
            var bytes: ByteArray
            do {
                val output = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                bytes = output.toByteArray()
                quality -= 7
            } while (bytes.size > 140_000 && quality >= 45)
            if (bytes.size > 150_000) return@withContext null
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) { null }
        finally {
            if (resized != null && resized !== cropped && resized !== source) resized.recycle()
            if (cropped != null && cropped !== source && cropped !== resized) cropped.recycle()
            if (source != null && source !== cropped && source !== resized) source.recycle()
        }
    }

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
                val compressedAvatar = if (s.selectedAvatarUri != null) compressAvatar(Uri.parse(s.selectedAvatarUri)) else null
                if (s.selectedAvatarUri != null && compressedAvatar == null) throw IllegalStateException("Не удалось обработать изображение. Выберите другой файл.")
                val result = api.updateProfile(authorization, YuberaProfileUpdateRequest(
                    bio = s.bio, visibility = s.visibility, avatarFrame = s.selectedFrame,
                    customName = s.customName.trim(), avatarAnimeId = s.selectedAvatarAnimeId,
                    avatarDataUrl = compressedAvatar
                ))
                if (!result.ok) throw IllegalStateException(result.error ?: "Не удалось сохранить профиль.")
                mutable.value = mutable.value.copy(saving = false, saved = true)
                onSaved()
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(saving = false, error = e.message ?: "Ошибка сохранения. Изменения не потеряны.")
            }
        }
    }
}
