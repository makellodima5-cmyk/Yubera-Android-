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
        assertEquals("user-1", store.storedAccountId)
        assertEquals("user-1", activeAccountStore.currentAccountId())
        assertNull(store.pending)
        assertTrue(api.exchangeCalled)
        assertFalse(activeAccountStore.claimedLegacyRows)
    }

    @Test
    fun existingSessionClaimsUnassignedRowsOnlyAfterServerIdentifiesItsOwner() = runTest {
        val store = FakeTokenStore().apply { token = "A".repeat(43) }
        val activeAccountStore = FakeActiveAccountStore(store, ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID)
        val restored = NativeAuthRepository(FakeApi(), store, activeAccountStore).hasRestorableSession()
        assertTrue(restored)
        assertEquals("user-1", store.storedAccountId)
        assertEquals("user-1", activeAccountStore.currentAccountId())
        assertTrue(activeAccountStore.claimedLegacyRows)
    }

    @Test
    fun storedAccountMismatchWithServerIdentityClearsSessionInsteadOfRebindingLocalData() = runTest {
        val store = FakeTokenStore().apply {
            token = "A".repeat(43)
            storedAccountId = "account-a"
        }
        val api = FakeApi().apply {
            sessionResponse = TelegramSessionResponse(
                authenticated = true,
                user = TelegramSessionUser(id = "account-b")
            )
        }
        val activeAccountStore = FakeActiveAccountStore(store, "account-a")

        val restored = NativeAuthRepository(api, store, activeAccountStore).hasRestorableSession()

        assertFalse(restored)
        assertNull(store.token)
        assertNull(store.storedAccountId)
        assertNull(activeAccountStore.currentAccountId())
        assertFalse(activeAccountStore.claimedLegacyRows)
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
        assertNull(store.storedAccountId)
        assertNull(activeAccountStore.currentAccountId())
    }

    @Test
    fun missingTokenCannotLeaveAnActiveAccountSelected() = runTest {
        val store = FakeTokenStore().apply { storedAccountId = "account-a" }
        val activeAccountStore = FakeActiveAccountStore(store, "account-a")

        val restored = NativeAuthRepository(FakeApi(), store, activeAccountStore).hasRestorableSession()

        assertFalse(restored)
        assertNull(store.storedAccountId)
        assertNull(activeAccountStore.currentAccountId())
    }

    @Test
    fun currentProfileExposesOnlyFieldsForTheStoredAccount() = runTest {
        val store = FakeTokenStore().apply {
            token = "A".repeat(43)
            storedAccountId = "user-1"
        }
        val user = TelegramSessionUser(
            id = "user-1",
            telegramId = "123456",
            firstName = "Алексей",
            lastName = "Иванов",
            username = "yubera_user",
            languageCode = "ru",
            createdAt = "2026-10-24 09:15:00"
        )
        val api = FakeApi().apply {
            sessionResponse = TelegramSessionResponse(authenticated = true, user = user)
        }

        val profile = NativeAuthRepository(api, store, FakeActiveAccountStore(store, "user-1")).getCurrentProfile()

        assertEquals(user, profile)
        assertEquals("2026-10-24 09:15:00", profile?.createdAt)
    }

    @Test
    fun currentProfileRejectsResponseWhenActiveAccountChangesInFlight() = runTest {
        val store = FakeTokenStore().apply {
            token = "A".repeat(43)
            storedAccountId = "user-1"
        }
        val activeAccountStore = FakeActiveAccountStore(store, "user-1")
        val api = FakeApi().apply {
            beforeSessionResponse = { activeAccountStore.switchTo("user-2") }
        }

        val profile = NativeAuthRepository(api, store, activeAccountStore).getCurrentProfile()

        assertNull(profile)
    }

    @Test
    fun currentProfileRejectsAResponseForAnotherAccount() = runTest {
        val store = FakeTokenStore().apply {
            token = "A".repeat(43)
            storedAccountId = "user-1"
        }
        val api = FakeApi().apply {
            sessionResponse = TelegramSessionResponse(
                authenticated = true,
                user = TelegramSessionUser(id = "user-2", firstName = "Другой пользователь")
            )
        }

        val profile = NativeAuthRepository(api, store, FakeActiveAccountStore(store)).getCurrentProfile()

        assertNull(profile)
    }


    @Test
    fun networkFailureDoesNotRestoreSessionWhenAccountBindingIsMissing() = runTest {
        val store = FakeTokenStore().apply { token = "A".repeat(43) }
        val activeAccountStore = FakeActiveAccountStore(store, "legacy-unassigned")
        val restored = NativeAuthRepository(FailingApi(), store, activeAccountStore).hasRestorableSession()
        assertFalse(restored)
    }

    @Test
    fun serverCanIdentifyExistingLegacyRowsWhenActiveAccountIsUnassigned() = runTest {
        val store = FakeTokenStore().apply { token = "A".repeat(43) }
        val activeAccountStore = FakeActiveAccountStore(store, ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID)
        val restored = NativeAuthRepository(FakeApi(), store, activeAccountStore).hasRestorableSession()
        assertTrue(restored)
        assertEquals("user-1", store.storedAccountId)
        assertTrue(activeAccountStore.claimedLegacyRows)
    }

    @Test
    fun refusesTokenWithoutStoredAccountOrLegacyUnassignedIdentity() = runTest {
        val store = FakeTokenStore().apply { token = "A".repeat(43) }
        val activeAccountStore = FakeActiveAccountStore(store, null)
        val restored = NativeAuthRepository(FakeApi(), store, activeAccountStore).hasRestorableSession()
        assertFalse(restored)
        assertNull(activeAccountStore.currentAccountId())
        assertNull(store.storedAccountId)
    }

    @Test
    fun networkFailureKeepsPreviouslyBoundLocalSessionAvailableOffline() = runTest {
        val store = FakeTokenStore().apply {
            token = "A".repeat(43)
            storedAccountId = "user-1"
        }
        val activeAccountStore = FakeActiveAccountStore(store, "user-1")
        val restored = NativeAuthRepository(FailingApi(), store, activeAccountStore).hasRestorableSession()
        assertTrue(restored)
    }


    @Test
    fun logoutClearsLocalCredentialsEvenWhenRemoteLogoutFails() = runTest {
        val store = FakeTokenStore().apply {
            token = "A".repeat(43)
            storedAccountId = "user-1"
            pending = PendingNativeAuth("state", "V".repeat(43), "login")
        }
        val activeAccountStore = FakeActiveAccountStore(store, "user-1")

        runCatching {
            NativeAuthRepository(FailingApi(), store, activeAccountStore).logout()
        }

        assertNull(store.token)
        assertNull(store.storedAccountId)
        assertNull(store.pending)
        assertNull(activeAccountStore.currentAccountId())
    }

    @Test
    fun clearingPendingAuthFailureDoesNotUndoSuccessfulCallback() = runTest {
        val store = FailingPendingClearTokenStore().apply {
            pending = PendingNativeAuth("expected-state", "V".repeat(43), "login")
        }
        val activeAccountStore = FakeActiveAccountStore(store)
        NativeAuthRepository(FakeApi(), store, activeAccountStore)
            .completeCallback("C".repeat(43), "expected-state")

        assertEquals("A".repeat(43), store.token)
        assertEquals("user-1", store.storedAccountId)
        assertEquals("user-1", activeAccountStore.currentAccountId())
    }

    private open class FakeTokenStore : SecureTokenStore {
        var token: String? = null
        var storedAccountId: String? = null
        var pending: PendingNativeAuth? = null
        override fun saveAccessToken(token: String) { this.token = token }
        override fun getAccessToken(): String? = token
        override fun clearAccessToken() { token = null }
        override fun saveAuthenticatedSession(token: String, accountId: String) {
            this.token = token
            this.storedAccountId = accountId
        }
        override fun clearAuthenticatedSession() {
            token = null
            storedAccountId = null
        }
        override fun getAccountId(): String? = storedAccountId
        override fun savePendingAuth(auth: PendingNativeAuth) { pending = auth }
        override fun getPendingAuth(): PendingNativeAuth? = pending
        override fun clearPendingAuth() { pending = null }
    }


    private class FailingPendingClearTokenStore : FakeTokenStore() {
        override fun clearPendingAuth() { error("local clear failed") }
    }

    private class FakeActiveAccountStore(
        private val tokenStore: FakeTokenStore,
        initialAccountId: String? = null
    ) : ActiveAccountStore {
        private val mutableAccountId = MutableStateFlow(initialAccountId)
        override val activeAccountId: StateFlow<String?> = mutableAccountId
        var claimedLegacyRows = false

        override fun currentAccountId(): String? = mutableAccountId.value
        fun switchTo(accountId: String?) { mutableAccountId.value = accountId }

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

    private class FailingApi : TelegramSessionApi {
        override suspend fun exchangeCode(request: MobileAuthExchangeRequest): MobileAuthExchangeResponse = error("offline")
        override suspend fun getSession(authorization: String): TelegramSessionResponse = error("offline")
        override suspend fun logout(authorization: String): TelegramSessionResponse = error("offline")
    }

    private class FakeApi : TelegramSessionApi {
        var exchangeCalled = false
        var sessionResponse = TelegramSessionResponse(
            authenticated = true,
            user = TelegramSessionUser(id = "user-1")
        )
        var beforeSessionResponse: (() -> Unit)? = null

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

        override suspend fun getSession(authorization: String): TelegramSessionResponse {
            beforeSessionResponse?.invoke()
            return sessionResponse
        }
        override suspend fun logout(authorization: String) = TelegramSessionResponse(authenticated = false)
    }
}
