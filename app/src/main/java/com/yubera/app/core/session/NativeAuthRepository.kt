package com.yubera.app.core.session

import com.yubera.app.data.auth.MobileAuthExchangeRequest
import com.yubera.app.data.auth.TelegramSessionApi
import com.yubera.app.data.auth.TelegramSessionUser
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
    private val tokenStore: SecureTokenStore,
    private val activeAccountStore: ActiveAccountStore
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
        val challenge = hex(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII)))
        tokenStore.savePendingAuth(PendingNativeAuth(state, verifier, flow))
        val params = linkedMapOf("native" to "1", "flow" to flow, "state" to state, "code_challenge" to challenge)
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
        val accountId = response.user.id.trim()
        require(accountId.isNotEmpty()) { "Сервер не вернул идентификатор аккаунта." }
        activeAccountStore.activateAccount(accountId, response.accessToken, claimLegacyRows = false)
        tokenStore.clearPendingAuth()
    }

    suspend fun hasRestorableSession(): Boolean {
        val token = tokenStore.getAccessToken()
        if (token == null) {
            activeAccountStore.clearActiveAccount()
            return false
        }

        return try {
            val response = api.getSession("Bearer $token")
            val accountId = response.user?.id?.trim().orEmpty()

            // A logout or account switch may have happened while the request was in flight.
            // Never let a stale response restore the old credentials.
            if (tokenStore.getAccessToken() != token) return false

            if (!response.authenticated || accountId.isEmpty()) {
                activeAccountStore.clearActiveAccount()
                false
            } else {
                val storedAccountId = tokenStore.getAccountId()
                val currentAccountId = activeAccountStore.currentAccountId()
                val legacyAccountPending = storedAccountId == null &&
                    currentAccountId == ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID
                val storedIdentityMatches = legacyAccountPending ||
                    (storedAccountId != null && storedAccountId == currentAccountId)
                if (!storedIdentityMatches) {
                    return false
                }
                if (storedAccountId != null && storedAccountId != accountId) {
                    activeAccountStore.clearActiveAccount()
                    return false
                }

                val claimLegacy = legacyAccountPending
                activeAccountStore.activateAccount(accountId, token, claimLegacyRows = claimLegacy)
                true
            }
        } catch (error: HttpException) {
            if (tokenStore.getAccessToken() != token) {
                false
            } else if (error.code() == 401) {
                activeAccountStore.clearActiveAccount()
                false
            } else {
                true
            }
        } catch (_: Exception) {
            // Preserve offline access only while the same stored token remains active.
            tokenStore.getAccessToken() == token
        }
    }

    /**
     * Fetches the public profile fields for the currently authenticated YUBERA account.
     * Both the bearer token and returned account ID are checked so a delayed response
     * cannot display profile information from an account that has since been switched.
     */
    suspend fun getCurrentProfile(): TelegramSessionUser? {
        val token = tokenStore.getAccessToken() ?: return null
        val accountId = tokenStore.getAccountId() ?: return null
        val response = api.getSession("Bearer $token")
        if (tokenStore.getAccessToken() != token) return null
        if (!response.authenticated) return null
        val user = response.user ?: return null
        return user.takeIf { it.id == accountId }
    }

    suspend fun logout() {
        val token = tokenStore.getAccessToken()
        try {
            if (token != null) api.logout("Bearer $token")
        } finally {
            activeAccountStore.clearActiveAccount()
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
    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val FLOW_LOGIN = "login"
        const val FLOW_REGISTER = "register"
        val EXCHANGE_CODE_PATTERN = Regex("^[A-Za-z0-9_-]{40,64}$")
        val secureRandom = SecureRandom()
    }
}
