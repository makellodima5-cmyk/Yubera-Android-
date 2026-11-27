package com.yubera.app.data.library

import androidx.room.withTransaction
import com.yubera.app.core.database.YuberaDatabase
import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.core.session.ActiveAccountStore
import com.yubera.app.features.library.LibrarySyncTrigger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RoomPersonalLibraryRepository @Inject constructor(
    private val database: YuberaDatabase,
    private val animeLibraryDao: AnimeLibraryDao,
    private val animeProgressDao: AnimeProgressDao,
    private val animeSubscriptionDao: AnimeSubscriptionDao,
    private val syncTrigger: LibrarySyncTrigger,
    private val activeAccountStore: ActiveAccountStore
) : PersonalLibraryRepository {
    private val mutationMutex = Mutex()

    override val favorites: Flow<List<AnimeLibraryEntity>> =
        activeAccountStore.activeAccountId.flatMapLatest { accountId ->
            if (accountId == null) flowOf(emptyList()) else animeLibraryDao.observeFavorites(accountId)
        }

    override val watchLater: Flow<List<AnimeLibraryEntity>> =
        activeAccountStore.activeAccountId.flatMapLatest { accountId ->
            if (accountId == null) flowOf(emptyList()) else animeLibraryDao.observeWatchLater(accountId)
        }

    override val progress: Flow<List<AnimeProgressEntity>> =
        activeAccountStore.activeAccountId.flatMapLatest { accountId ->
            if (accountId == null) flowOf(emptyList()) else animeProgressDao.observeAll(accountId)
        }

    override val subscriptions: Flow<List<AnimeSubscriptionEntity>> =
        activeAccountStore.activeAccountId.flatMapLatest { accountId ->
            if (accountId == null) flowOf(emptyList()) else animeSubscriptionDao.observeAll(accountId)
        }

    override suspend fun setFavorite(animeId: String, isFavorite: Boolean, updatedAtEpochMillis: Long) =
        mutateAccountData(animeId) { accountId ->
            val current = animeLibraryDao.getByAnimeId(accountId, animeId)
            val timestamp = nextTimestamp(updatedAtEpochMillis, current?.favoriteUpdatedAtEpochMillis)
            animeLibraryDao.upsert(AnimeLibraryEntity(
                animeId = animeId,
                isFavorite = isFavorite,
                isInWatchLater = current?.isInWatchLater ?: false,
                updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, timestamp),
                favoriteUpdatedAtEpochMillis = timestamp,
                watchLaterUpdatedAtEpochMillis = current?.watchLaterUpdatedAtEpochMillis ?: 0L,
                accountId = accountId
            ))
        }

    override suspend fun setWatchLater(animeId: String, isInWatchLater: Boolean, updatedAtEpochMillis: Long) =
        mutateAccountData(animeId) { accountId ->
            val current = animeLibraryDao.getByAnimeId(accountId, animeId)
            val timestamp = nextTimestamp(updatedAtEpochMillis, current?.watchLaterUpdatedAtEpochMillis)
            animeLibraryDao.upsert(AnimeLibraryEntity(
                animeId = animeId,
                isFavorite = current?.isFavorite ?: false,
                isInWatchLater = isInWatchLater,
                updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, timestamp),
                favoriteUpdatedAtEpochMillis = current?.favoriteUpdatedAtEpochMillis ?: 0L,
                watchLaterUpdatedAtEpochMillis = timestamp,
                accountId = accountId
            ))
        }

    override suspend fun saveProgress(progress: AnimeProgressEntity) =
        mutateAccountData(progress.animeId) { accountId ->
            val current = animeProgressDao.getByAnimeId(accountId, progress.animeId)
            animeProgressDao.upsert(progress.copy(
                accountId = accountId,
                updatedAtEpochMillis = nextTimestamp(progress.updatedAtEpochMillis, current?.updatedAtEpochMillis)
            ))
        }

    override suspend fun subscribeToAnime(animeId: String, notificationsEnabled: Boolean, updatedAtEpochMillis: Long) =
        mutateAccountData(animeId) { accountId ->
            val current = animeSubscriptionDao.getByAnimeId(accountId, animeId)
            animeSubscriptionDao.upsert(AnimeSubscriptionEntity(
                animeId = animeId,
                notificationsEnabled = notificationsEnabled,
                updatedAtEpochMillis = nextTimestamp(updatedAtEpochMillis, current?.updatedAtEpochMillis),
                lastKnownEpisodesAired = current?.lastKnownEpisodesAired,
                isSubscribed = true,
                accountId = accountId
            ))
        }

    override suspend fun setSubscriptionNotificationsEnabled(animeId: String, enabled: Boolean, updatedAtEpochMillis: Long) =
        mutateAccountData(animeId) { accountId ->
            val current = animeSubscriptionDao.getByAnimeId(accountId, animeId) ?: return@mutateAccountData
            if (!current.isSubscribed) return@mutateAccountData
            animeSubscriptionDao.upsert(current.copy(
                notificationsEnabled = enabled,
                updatedAtEpochMillis = nextTimestamp(updatedAtEpochMillis, current.updatedAtEpochMillis)
            ))
        }

    override suspend fun removeSubscription(animeId: String) =
        mutateAccountData(animeId) { accountId ->
            val current = animeSubscriptionDao.getByAnimeId(accountId, animeId)
            val timestamp = nextTimestamp(System.currentTimeMillis(), current?.updatedAtEpochMillis)
            if (current == null) {
                animeSubscriptionDao.upsert(AnimeSubscriptionEntity(
                    animeId = animeId,
                    notificationsEnabled = false,
                    updatedAtEpochMillis = timestamp,
                    isSubscribed = false,
                    accountId = accountId
                ))
            } else {
                animeSubscriptionDao.upsert(current.copy(
                    notificationsEnabled = false,
                    isSubscribed = false,
                    updatedAtEpochMillis = timestamp
                ))
            }
        }

    private suspend fun mutateAccountData(
        animeId: String,
        mutation: suspend (String) -> Unit
    ) = mutationMutex.withLock {
        require(animeId.isNotBlank()) { "ID аниме не может быть пустым." }
        val accountId = requireAccountId()
        database.withTransaction {
            // Re-check after taking the mutation lock. A concurrent account switch
            // must never write one account's interaction into another account.
            check(activeAccountStore.currentAccountId() == accountId) {
                "Аккаунт изменился до сохранения. Повторите действие."
            }
            mutation(accountId)
        }
        syncTrigger.enqueue()
    }

    private fun nextTimestamp(requested: Long, current: Long?): Long =
        maxOf(requested, (current ?: 0L) + 1L)

    private fun requireAccountId(): String =
        activeAccountStore.currentAccountId()?.takeIf { it.isNotBlank() && it != ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID }
            ?: throw IllegalStateException("Войдите в аккаунт, чтобы изменять личную библиотеку.")
}
