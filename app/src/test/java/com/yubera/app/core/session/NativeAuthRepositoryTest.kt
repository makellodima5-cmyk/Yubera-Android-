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
        val repository = NativeAuthRepository(FakeApi(), store)
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
        val repository = NativeAuthRepository(FakeApi(), FakeTokenStore())
        var rejected = false
        try {
            repository.createAuthorizationUrl("register", privacyConsent = true, termsAccepted = false)
        } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test
    fun callbackMustMatchStoredStateBeforeExchanging() = runTest {
        val store = FakeTokenStore().apply { pending = PendingNativeAuth("expected-state", "V".repeat(43), "login") }
        val api = FakeApi()
        val repository = NativeAuthRepository(api, store)
        var rejected = false
        try { repository.completeCallback("C".repeat(43), "wrong-state") } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertFalse(api.exchangeCalled)
    }

    @Test
    fun successfulCallbackStoresBearerTokenAndClearsPendingState() = runTest {
        val store = FakeTokenStore().apply { pending = PendingNativeAuth("expected-state", "V".repeat(43), "login") }
        val api = FakeApi()
        NativeAuthRepository(api, store).completeCallback("C".repeat(43), "expected-state")
        assertEquals("A".repeat(43), store.token)
        assertNull(store.pending)
        assertTrue(api.exchangeCalled)
    }

    private class FakeTokenStore : SecureTokenStore {
        var token: String? = null
        var pending: PendingNativeAuth? = null
        override fun saveAccessToken(token: String) { this.token = token }
        override fun getAccessToken(): String? = token
        override fun clearAccessToken() { token = null }
        override fun savePendingAuth(auth: PendingNativeAuth) { pending = auth }
        override fun getPendingAuth(): PendingNativeAuth? = pending
        override fun clearPendingAuth() { pending = null }
    }

    private class FakeApi : TelegramSessionApi {
        var exchangeCalled = false
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
        override suspend fun getSession(authorization: String) = TelegramSessionResponse(authenticated = true)
        override suspend fun logout(authorization: String) = TelegramSessionResponse(authenticated = false)
    }
}
