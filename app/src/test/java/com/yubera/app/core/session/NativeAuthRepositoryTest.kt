package com.yubera.app.core.session

import com.yubera.app.data.auth.MobileAuthExchangeRequest
import com.yubera.app.data.auth.MobileAuthExchangeResponse
import com.yubera.app.data.auth.TelegramSessionApi
import com.yubera.app.data.auth.TelegramSessionResponse
import com.yubera.app.data.auth.TelegramSessionUser
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthRepositoryTest {
    @Test
    fun createsPkceAuthorizationUrlAndStoresVerifier() {
        val store = FakeTokenStore()
        val repository = NativeAuthRepository(FakeApi(), store, FakeActiveAccountStore(store))
        val query = URI(repository.createAuthorizationUrl("login")).rawQuery.split("&").associate {
            val parts = it.split("=", limit = 2)
            URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
        }
        val pending = store.pending
        assertEquals("login", query["flow"])
        assertEquals("1", query["native"])
        assertEquals(pending?.state, query["state"])
        assertNotNull(pending)
        val expectedChallenge = MessageDigest.getInstance("SHA-256").digest(pending!!.verifier.toByteArray(StandardCharsets.US_ASCII))
            .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
        assertEquals(expectedChallenge, query["code_challenge"])
    }

    @Test
    fun registrationRequiresBothConsents() {
        val store = FakeTokenStore()
        val repository = NativeAuthRepository(FakeApi(), store, FakeActiveAccountStore(store))
        var rejected = false
        try {
            repository.createAuthorizationUrl("register", privacyConsent = true, termsAccepted = false)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun callbackMustMatchStoredStateBeforeExchanging() = runTest {
        val store = FakeTokenStore().apply { pending = PendingNativeAuth("expected-state", "V".repeat(43), "login") }
        val api = FakeApi()
        val repository = NativeAuthRepository(api, store, FakeActiveAccountStore(store))
        var rejected = false
        try {
            repository.completeCallback("C".repeat(43), "wrong-state")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertFalse(api.exchangeCalled)
    }

    @Test
    fun successfulCallbackStoresTokenAndBindsItToTheReturnedAccount() = runTest {
        val store = FakeTokenStore().apply { pending = PendingNativeAuth("expected-state", "V".repeat(43), "login") }
        val api = FakeApi()
        val activeAccountStore = FakeActiveAccountStore(store)
        NativeAuthRepository(api, store, activeAccountStore).completeCallback("C".repeat(43), "expected-state")
        assertEquals("A".repeat(43), store.token)
        assertEquals("user-1", store.accountId)
        assertEquals("user-1", activeAccountStore.currentAccountId())
        assertNull(store.pending)
        assertTrue(api.exchangeCalled)
        assertFalse(activeAccountStore.claimedLegacyRows)
    }

    @Test
    fun existingSessionClaimsUnassignedRowsOnlyAfterServerIdentifiesItsOwner() = runTest {
        val store = FakeTokenStore().apply { token = "A".repeat(43) }
        val activeAccountStore = FakeActiveAccountStore(
            store,
            ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID
        )
        val restored = NativeAuthRepository(FakeApi(), store, activeAccountStore).hasRestorableSession()
        assertTrue(restored)
        assertEquals("user-1", store.accountId)
        assertEquals("user-1", activeAccountStore.currentAccountId())
        assertTrue(activeAccountStore.claimedLegacyRows)
    }

    @Test
    fun authenticatedResponseWithoutAccountIdentityFailsClosed() = runTest {
        val store = FakeTokenStore().apply { token = "A".repeat(43) }
        val api = FakeApi().apply {
            sessionResponse = TelegramSessionResponse(authenticated = true, user = null)
        }
        val activeAccountStore = FakeActiveAccountStore(store, "user-before")
        val restored = NativeAuthRepository(api, store, activeAccountStore).hasRestorableSession()
        assertFalse(restored)
        assertNull(store.token)
        assertNull(store.accountId)
        assertNull(activeAccountStore.currentAccountId())
    }

    private class FakeTokenStore : SecureTokenStore {
        var token: String? = null
        var accountId: String? = null
        var pending: PendingNativeAuth? = null
        override fun saveAccessToken(token: String) { this.token = token }
        override fun getAccessToken(): String? = token
        override fun clearAccessToken() { token = null }
        override fun saveAuthenticatedSession(token: String, accountId: String) {
            this.token = token
            this.accountId = accountId
        }
        override fun clearAuthenticatedSession() {
            token = null
            accountId = null
        }
        override fun getAccountId(): String? = accountId
        override fun savePendingAuth(auth: PendingNativeAuth) { pending = auth }
        override fun getPendingAuth(): PendingNativeAuth? = pending
        override fun clearPendingAuth() { pending = null }
    }

    private class FakeActiveAccountStore(
        private val tokenStore: FakeTokenStore,
        initialAccountId: String? = null
    ) : ActiveAccountStore {
        private val mutableAccountId = MutableStateFlow(initialAccountId)
        override val activeAccountId: StateFlow<String?> = mutableAccountId
        var claimedLegacyRows = false

        override fun currentAccountId(): String? = mutableAccountId.value

        override suspend fun activateAccount(accountId: String, accessToken: String, claimLegacyRows: Boolean) {
            if (claimLegacyRows) claimedLegacyRows = true
            tokenStore.saveAuthenticatedSession(accessToken, accountId)
            mutableAccountId.value = accountId
        }

        override suspend fun clearActiveAccount() {
            tokenStore.clearAuthenticatedSession()
            mutableAccountId.value = null
        }
    }

    private class FakeApi : TelegramSessionApi {
        var exchangeCalled = false
        var sessionResponse = TelegramSessionResponse(
            authenticated = true,
            user = TelegramSessionUser(id = "user-1")
        )

        override suspend fun exchangeCode(request: MobileAuthExchangeRequest): MobileAuthExchangeResponse {
            exchangeCalled = true
            assertEquals("C".repeat(43), request.code)
            assertEquals("V".repeat(43), request.codeVerifier)
            return MobileAuthExchangeResponse(
                authenticated = true,
                accessToken = "A".repeat(43),
                tokenType = "Bearer",
                expiresAt = 1_900_000_000_000,
                user = TelegramSessionUser(id = "user-1")
            )
        }

        override suspend fun getSession(authorization: String) = sessionResponse
        override suspend fun logout(authorization: String) = TelegramSessionResponse(authenticated = false)
    }
}
