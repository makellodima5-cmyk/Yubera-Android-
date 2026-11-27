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
        require(pending.flow == FLOW_LOGIN || pending.flow == FLOW_REGISTER) { "Неизвестный тип авторизации." }

        val response = api.exchangeCode(MobileAuthExchangeRequest(code, pending.verifier))
        require(response.authenticated && response.tokenType.equals("Bearer", ignoreCase = true)) {
            "Сервер не подтвердил авторизацию."
        }
        require(response.accessToken.matches(EXCHANGE_CODE_PATTERN)) { "Сервер вернул некорректную сессию." }
        val accountId = response.user.id.trim()
        require(accountId.isNotEmpty()) { "Сервер не вернул идентификатор аккаунта." }

        activeAccountStore.activateAccount(accountId, response.accessToken, claimLegacyRows = false)
        // A failed cleanup must not report a failed login after a session has been saved.
        runCatching { tokenStore.clearPendingAuth() }
    }

    suspend fun hasRestorableSession(): Boolean {
        val token = tokenStore.getAccessToken()
        if (token == null) {
            activeAccountStore.clearActiveAccount()
            return false
        }

        val storedAccountId = tokenStore.getAccountId()
        val initialAccountId = activeAccountStore.currentAccountId()
        val legacyAccountPending = storedAccountId == null &&
            initialAccountId == ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID
        val previouslyBoundSession = storedAccountId != null &&
            storedAccountId.isNotBlank() &&
            storedAccountId == initialAccountId

        if (!legacyAccountPending && !previouslyBoundSession) {
            activeAccountStore.clearActiveAccount()
            return false
        }

        return try {
            val response = api.getSession("Bearer $token")

            // Do not let a slow response restore credentials after logout or account switch.
            if (!sessionSnapshotIsCurrent(token, storedAccountId, initialAccountId)) return false

            val serverAccountId = response.user?.id?.trim().orEmpty()
            if (!response.authenticated || serverAccountId.isEmpty()) {
                activeAccountStore.clearActiveAccount()
                false
            } else if (storedAccountId != null && storedAccountId != serverAccountId) {
                activeAccountStore.clearActiveAccount()
                false
            } else {
                activeAccountStore.activateAccount(
                    serverAccountId,
                    token,
                    claimLegacyRows = legacyAccountPending
                )
                true
            }
        } catch (error: HttpException) {
            if (!sessionSnapshotIsCurrent(token, storedAccountId, initialAccountId)) {
                false
            } else if (error.code() == 401) {
                activeAccountStore.clearActiveAccount()
                false
            } else {
                // A session already bound to an account remains available offline.
                previouslyBoundSession
            }
        } catch (_: Exception) {
            sessionSnapshotIsCurrent(token, storedAccountId, initialAccountId) && previouslyBoundSession
        }
    }

    fun getSessionAuthorization(): String? = tokenStore.getAccessToken()?.let { "Bearer $it" }

    suspend fun getCurrentProfile(): TelegramSessionUser? {
        val token = tokenStore.getAccessToken() ?: return null
        val accountId = tokenStore.getAccountId()?.takeIf(String::isNotBlank) ?: return null
        if (activeAccountStore.currentAccountId() != accountId) return null

        val response = api.getSession("Bearer $token")
        if (tokenStore.getAccessToken() != token ||
            tokenStore.getAccountId() != accountId ||
            activeAccountStore.currentAccountId() != accountId
        ) return null
        if (!response.authenticated) return null
        return response.user?.takeIf { it.id == accountId }
    }

    suspend fun logout() {
        val token = tokenStore.getAccessToken()
        try {
            if (token != null) api.logout("Bearer $token")
        } finally {
            try {
                activeAccountStore.clearActiveAccount()
            } finally {
                tokenStore.clearPendingAuth()
            }
        }
    }

    fun clearPendingAuth() = tokenStore.clearPendingAuth()

    private fun sessionSnapshotIsCurrent(
        token: String,
        accountId: String?,
        activeAccountId: String?
    ): Boolean =
        tokenStore.getAccessToken() == token &&
            tokenStore.getAccountId() == accountId &&
            activeAccountStore.currentAccountId() == activeAccountId

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
