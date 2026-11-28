package com.yubera.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yubera.app.core.session.NativeAuthRepository
import com.yubera.app.data.auth.YuberaProfileApi
import com.yubera.app.data.auth.YuberaCommunityProfile
import com.yubera.app.data.auth.YuberaCommunityResponse
import com.yubera.app.data.auth.YuberaProfileUpdateRequest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutable = MutableStateFlow(ProfileEditState())
    val state = mutable.asStateFlow()
    private val profileCache = context.getSharedPreferences("yubera_profile_cache", Context.MODE_PRIVATE)
    private val profileJson = Json { ignoreUnknownKeys = true }

    fun load() {
        if (mutable.value.loading && mutable.value.profile != null) return
        mutable.value = mutable.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val accountId = auth.getStoredAccountId()
            if (accountId != null) {
                profileCache.getString("community_${accountId}", null)?.let { cached ->
                    runCatching { profileJson.decodeFromString<YuberaCommunityResponse>(cached) }
                        .getOrNull()?.profile?.let { p ->
                            if (mutable.value.profile == null) mutable.value = mutable.value.copy(
                                profile = p, bio = p.bio, visibility = p.visibility, selectedFrame = p.avatarFrame,
                                category = initialFrameCategory(p)
                            )
                        }
                }
            }
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
                    profile = p, bio = p.bio, visibility = p.visibility, selectedFrame = p.avatarFrame,
                    category = profileFrameCategory(p.avatarFrame, p.isAdmin), loading = false
                )
                if (accountId != null) profileCache.edit().putString("community_${accountId}", profileJson.encodeToString(result)).apply()
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(loading = false, error = "Не удалось загрузить профиль. Сохранённые данные доступны офлайн.")
            }
        }
    }

    fun setBio(value: String) = mutable.value.let { mutable.value = it.copy(bio = value.take(250)) }
    fun setVisibility(value: String) = mutable.value.let { mutable.value = it.copy(visibility = value) }
    fun setCategory(value: String) = mutable.value.let { mutable.value = it.copy(category = value) }
    fun setFrame(value: String?) = mutable.value.let { mutable.value = it.copy(selectedFrame = value) }
    fun setAvatarUri(value: String?) = mutable.value.let { mutable.value = it.copy(selectedAvatarUri = value) }

    private fun initialFrameCategory(profile: YuberaCommunityProfile): String = profileFrameCategory(profile.avatarFrame, profile.isAdmin)

internal fun profileFrameCategory(frame: String?, isAdmin: Boolean): String = when {
    frame?.startsWith("Admin_Frame_") == true || frame == "Ramka_Admin_01.webp" -> if (isAdmin) "admin" else "support"
    frame?.startsWith("YUBERA_Plus_Avatar_Frame_") == true -> "plus"
    else -> "support"
}

private suspend fun compressAvatar(uri: Uri): String? = withContext(Dispatchers.IO) {
        var source: Bitmap? = null
        var cropped: Bitmap? = null
        var resized: Bitmap? = null
        try {
            val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            if (raw.size > 2 * 1024 * 1024) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
            var sampleSize = 1
            val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxDimension / sampleSize > 1024) sampleSize *= 2
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize.coerceAtLeast(1) }
            val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, decodeOptions) ?: return@withContext null
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
