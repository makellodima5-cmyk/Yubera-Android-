package com.yubera.app.core.session

import kotlinx.coroutines.flow.StateFlow

interface ActiveAccountStore {
    val activeAccountId: StateFlow<String?>
    fun currentAccountId(): String?
    suspend fun activateAccount(accountId: String, accessToken: String, claimLegacyRows: Boolean)
    suspend fun clearActiveAccount()

    companion object {
        const val LEGACY_UNASSIGNED_ACCOUNT_ID = "legacy-unassigned"
    }
}
