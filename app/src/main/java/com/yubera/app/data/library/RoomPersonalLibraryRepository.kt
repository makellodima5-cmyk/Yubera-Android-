package com.yubera.app.data.library

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class RoomPersonalLibraryRepository @Inject constructor(
    private val animeLibraryDao: AnimeLibraryDao,
    private val animeProgressDao: AnimeProgressDao,
    private val animeSubscriptionDao: AnimeSubscriptionDao,
    private val syncTrigger: LibrarySyncTrigger,
    private val activeAccountStore: ActiveAccountStore
) : PersonalLibraryRepository {

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

    override suspend fun setFavorite(animeId: String, isFavorite: Boolean, updatedAtEpochMillis: Long) {
        val accountId = requireAccountId()
        val current = animeLibraryDao.getByAnimeId(accountId, animeId)
        val favoriteTimestamp = maxOf(updatedAtEpochMillis, (current?.favoriteUpdatedAtEpochMillis ?: 0L) + 1L)
        animeLibraryDao.upsert(AnimeLibraryEntity(
            animeId = animeId,
            isFavorite = isFavorite,
            isInWatchLater = current?.isInWatchLater ?: false,
            updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, favoriteTimestamp),
            favoriteUpdatedAtEpochMillis = favoriteTimestamp,
            watchLaterUpdatedAtEpochMillis = current?.watchLaterUpdatedAtEpochMillis ?: 0L,
            accountId = accountId
        ))
        syncTrigger.enqueue()
    }

    override suspend fun setWatchLater(animeId: String, isInWatchLater: Boolean, updatedAtEpochMillis: Long) {
        val accountId = requireAccountId()
        val current = animeLibraryDao.getByAnimeId(accountId, animeId)
        val timestamp = maxOf(updatedAtEpochMillis, (current?.watchLaterUpdatedAtEpochMillis ?: 0L) + 1L)
        animeLibraryDao.upsert(AnimeLibraryEntity(
            animeId = animeId,
            isFavorite = current?.isFavorite ?: false,
            isInWatchLater = isInWatchLater,
            updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, timestamp),
            favoriteUpdatedAtEpochMillis = current?.favoriteUpdatedAtEpochMillis ?: 0L,
            watchLaterUpdatedAtEpochMillis = timestamp,
            accountId = accountId
        ))
        syncTrigger.enqueue()
    }

    override suspend fun saveProgress(progress: AnimeProgressEntity) {
        val accountId = requireAccountId()
        val current = animeProgressDao.getByAnimeId(accountId, progress.animeId)
        animeProgressDao.upsert(progress.copy(
            accountId = accountId,
            updatedAtEpochMillis = maxOf(progress.updatedAtEpochMillis, (current?.updatedAtEpochMillis ?: 0L) + 1L)
        ))
        syncTrigger.enqueue()
    }

    override suspend fun subscribeToAnime(animeId: String, notificationsEnabled: Boolean, updatedAtEpochMillis: Long) {
        val accountId = requireAccountId()
        val current = animeSubscriptionDao.getByAnimeId(accountId, animeId)
        val timestamp = maxOf(updatedAtEpochMillis, (current?.updatedAtEpochMillis ?: 0L) + 1L)
        animeSubscriptionDao.upsert(AnimeSubscriptionEntity(
            animeId = animeId,
            notificationsEnabled = notificationsEnabled,
            updatedAtEpochMillis = timestamp,
            lastKnownEpisodesAired = current?.lastKnownEpisodesAired,
            isSubscribed = true,
            accountId = accountId
        ))
        syncTrigger.enqueue()
    }

    override suspend fun setSubscriptionNotificationsEnabled(animeId: String, enabled: Boolean, updatedAtEpochMillis: Long) {
        val accountId = requireAccountId()
        val current = animeSubscriptionDao.getByAnimeId(accountId, animeId) ?: return
        if (!current.isSubscribed) return
        val timestamp = maxOf(updatedAtEpochMillis, current.updatedAtEpochMillis + 1L)
        animeSubscriptionDao.upsert(current.copy(notificationsEnabled = enabled, updatedAtEpochMillis = timestamp))
        syncTrigger.enqueue()
    }

    override suspend fun removeSubscription(animeId: String) {
        val accountId = requireAccountId()
        val current = animeSubscriptionDao.getByAnimeId(accountId, animeId)
        val timestamp = maxOf(System.currentTimeMillis(), (current?.updatedAtEpochMillis ?: 0L) + 1L)
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
        syncTrigger.enqueue()
    }

    private fun requireAccountId(): String =
        activeAccountStore.currentAccountId()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Войдите в аккаунт, чтобы изменять личную библиотеку.")
}
