package com.yubera.app.core.session

import androidx.room.withTransaction
import com.yubera.app.core.database.YuberaDatabase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RoomActiveAccountStore @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val database: YuberaDatabase
) : ActiveAccountStore {
    private val mutex = Mutex()
    private val initialAccountId = tokenStore.getAccountId()
        ?: if (tokenStore.getAccessToken() != null) ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID else null
    private val mutableAccountId = MutableStateFlow(initialAccountId)

    override val activeAccountId = mutableAccountId.asStateFlow()
    override fun currentAccountId(): String? = mutableAccountId.value

    override suspend fun activateAccount(accountId: String, accessToken: String, claimLegacyRows: Boolean) {
        require(accountId.isNotBlank() && accessToken.isNotBlank())
        mutex.withLock {
            val previousAccountId = mutableAccountId.value
            mutableAccountId.value = null
            try {
                if (claimLegacyRows) {
                    database.withTransaction {
                        database.animeLibraryDao().claimLegacyRows(accountId)
                        database.animeProgressDao().claimLegacyRows(accountId)
                        database.animeSubscriptionDao().claimLegacyRows(accountId)
                        database.animeEpisodeNotificationDao().claimLegacyRows(accountId)
                    }
                }
                tokenStore.saveAuthenticatedSession(accessToken, accountId)
                mutableAccountId.value = accountId
            } catch (error: Exception) {
                mutableAccountId.value = previousAccountId
                throw error
            }
        }
    }

    override suspend fun clearActiveAccount() {
        mutex.withLock {
            mutableAccountId.value = null
            tokenStore.clearAuthenticatedSession()
        }
    }
}
