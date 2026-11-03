package com.yubera.app.core.session

import com.yubera.app.data.auth.MobileAuthExchangeRequest
import com.yubera.app.data.auth.TelegramSessionApi
import com.yubera.app.di.NetworkModule
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class NativeAuthRepository @Inject constructor(
    private val api: TelegramSessionApi,
    private val tokenStore: SecureTokenStore
) {
    fun createAuthorizationUrl(
        flow: String,
        privacyConsent: Boolean = false,
        termsAccepted: Boolean = false
    ): String {
        require(flow == FLOW_LOGIN || flow == FLOW_REGISTER) { "Неизвестный тип авторизации." }
        require(flow != FLOW_REGISTER || (privacyConsent && termsAccepted)) {
            "Для регистрации необходимо принять условия и политику конфиденциальности."
        }
        val verifier = randomToken()
        val state = randomToken()
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII)))
        tokenStore.savePendingAuth(PendingNativeAuth(state, verifier, flow))
        val params = linkedMapOf(
            "native" to "1",
            "flow" to flow,
            "state" to state,
            "code_challenge" to challenge
        )
        if (flow == FLOW_REGISTER) {
            params["privacy_consent"] = "true"
            params["terms_accepted"] = "true"
        }
        val query = params.entries.joinToString("&") { (key, value) -> encode(key) + "=" + encode(value) }
        return NetworkModule.YUBERA_API_BASE_URL + "api/telegram/oidc?" + query
    }

    suspend fun completeCallback(code: String, state: String) {
        require(code.matches(EXCHANGE_CODE_PATTERN)) { "Некорректный код авторизации." }
        val pending = tokenStore.getPendingAuth() ?: throw IllegalStateException("Сессия авторизации не найдена.")
        require(state == pending.state) { "Проверка состояния авторизации не пройдена." }
        val response = api.exchangeCode(MobileAuthExchangeRequest(code, pending.verifier))
        require(response.authenticated && response.tokenType.equals("Bearer", ignoreCase = true)) {
            "Сервер не подтвердил авторизацию."
        }
        require(response.accessToken.matches(EXCHANGE_CODE_PATTERN)) { "Сервер вернул некорректную сессию." }
        tokenStore.saveAccessToken(response.accessToken)
        tokenStore.clearPendingAuth()
    }

    suspend fun hasRestorableSession(): Boolean {
        val token = tokenStore.getAccessToken() ?: return false
        return try {
            val response = api.getSession("Bearer $token")
            if (response.authenticated) true else {
                tokenStore.clearAccessToken()
                false
            }
        } catch (error: HttpException) {
            if (error.code() == 401) {
                tokenStore.clearAccessToken()
                false
            } else true
        } catch (_: Exception) {
            true
        }
    }

    suspend fun logout() {
        val token = tokenStore.getAccessToken()
        try {
            if (token != null) api.logout("Bearer $token")
        } finally {
            tokenStore.clearAccessToken()
            tokenStore.clearPendingAuth()
        }
    }

    fun clearPendingAuth() = tokenStore.clearPendingAuth()

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return base64Url(bytes)
    }

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private companion object {
        const val FLOW_LOGIN = "login"
        const val FLOW_REGISTER = "register"
        val EXCHANGE_CODE_PATTERN = Regex("^[A-Za-z0-9_-]{40,64}$")
        val secureRandom = SecureRandom()
    }
}
