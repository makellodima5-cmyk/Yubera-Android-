package com.yubera.app.core.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class AndroidKeystoreTokenStore @Inject constructor(
    @ApplicationContext context: Context
) : SecureTokenStore {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun saveAccessToken(token: String) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, encrypt(token)).apply()
    }

    @Synchronized
    override fun getAccessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)?.let { encrypted ->
        try { decrypt(encrypted) } catch (_: Exception) {
            preferences.edit().remove(KEY_ACCESS_TOKEN).apply()
            null
        }
    }

    @Synchronized
    override fun clearAccessToken() {
        preferences.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    @Synchronized
    override fun savePendingAuth(auth: PendingNativeAuth) {
        val json = JSONObject()
            .put("state", auth.state)
            .put("verifier", auth.verifier)
            .put("flow", auth.flow)
            .toString()
        preferences.edit().putString(KEY_PENDING_AUTH, encrypt(json)).apply()
    }

    @Synchronized
    override fun getPendingAuth(): PendingNativeAuth? {
        val encrypted = preferences.getString(KEY_PENDING_AUTH, null) ?: return null
        return try {
            val json = JSONObject(decrypt(encrypted))
            PendingNativeAuth(json.getString("state"), json.getString("verifier"), json.getString("flow"))
        } catch (_: Exception) {
            preferences.edit().remove(KEY_PENDING_AUTH).apply()
            null
        }
    }

    @Synchronized
    override fun clearPendingAuth() {
        preferences.edit().remove(KEY_PENDING_AUTH).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val combined = cipher.iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > GCM_IV_BYTES)
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val encrypted = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "yubera_secure_auth"
        const val KEY_ACCESS_TOKEN = "access_token_encrypted"
        const val KEY_PENDING_AUTH = "pending_auth_encrypted"
        const val KEY_ALIAS = "com.yubera.app.auth.aes"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
