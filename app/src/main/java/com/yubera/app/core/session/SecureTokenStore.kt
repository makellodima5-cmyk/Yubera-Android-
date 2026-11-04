package com.yubera.app.core.session

data class PendingNativeAuth(val state: String, val verifier: String, val flow: String)

interface SecureTokenStore {
    fun saveAccessToken(token: String)
    fun getAccessToken(): String?
    fun clearAccessToken()
    fun saveAuthenticatedSession(token: String, accountId: String)
    fun clearAuthenticatedSession()
    fun getAccountId(): String?
    fun savePendingAuth(auth: PendingNativeAuth)
    fun getPendingAuth(): PendingNativeAuth?
    fun clearPendingAuth()
}
