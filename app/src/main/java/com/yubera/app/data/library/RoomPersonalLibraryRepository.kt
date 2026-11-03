package com.yubera.app.data.library

import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.features.library.LibrarySyncTrigger
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RoomPersonalLibraryRepository @Inject constructor(
    private val animeLibraryDao: AnimeLibraryDao,
    private val animeProgressDao: AnimeProgressDao,
    private val animeSubscriptionDao: AnimeSubscriptionDao,
    private val syncTrigger: LibrarySyncTrigger
) : PersonalLibraryRepository {

    override val favorites: Flow<List<AnimeLibraryEntity>> = animeLibraryDao.observeFavorites()
    override val watchLater: Flow<List<AnimeLibraryEntity>> = animeLibraryDao.observeWatchLater()
    override val progress: Flow<List<AnimeProgressEntity>> = animeProgressDao.observeAll()
    override val subscriptions: Flow<List<AnimeSubscriptionEntity>> = animeSubscriptionDao.observeAll()

    override suspend fun setFavorite(animeId: String, isFavorite: Boolean, updatedAtEpochMillis: Long) {
        val current = animeLibraryDao.getByAnimeId(animeId)
        val favoriteTimestamp = maxOf(updatedAtEpochMillis, (current?.favoriteUpdatedAtEpochMillis ?: 0L) + 1L)
        animeLibraryDao.upsert(
            AnimeLibraryEntity(
                animeId = animeId,
                isFavorite = isFavorite,
                isInWatchLater = current?.isInWatchLater ?: false,
                updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, favoriteTimestamp),
                favoriteUpdatedAtEpochMillis = favoriteTimestamp,
                watchLaterUpdatedAtEpochMillis = current?.watchLaterUpdatedAtEpochMillis ?: 0L
            )
        )
        syncTrigger.enqueue()
    }

    override suspend fun setWatchLater(animeId: String, isInWatchLater: Boolean, updatedAtEpochMillis: Long) {
        val current = animeLibraryDao.getByAnimeId(animeId)
        val watchLaterTimestamp = maxOf(updatedAtEpochMillis, (current?.watchLaterUpdatedAtEpochMillis ?: 0L) + 1L)
        animeLibraryDao.upsert(
            AnimeLibraryEntity(
                animeId = animeId,
                isFavorite = current?.isFavorite ?: false,
                isInWatchLater = isInWatchLater,
                updatedAtEpochMillis = maxOf(current?.updatedAtEpochMillis ?: 0L, watchLaterTimestamp),
                favoriteUpdatedAtEpochMillis = current?.favoriteUpdatedAtEpochMillis ?: 0L,
                watchLaterUpdatedAtEpochMillis = watchLaterTimestamp
            )
        )
        syncTrigger.enqueue()
    }

    override suspend fun saveProgress(progress: AnimeProgressEntity) {
        val current = animeProgressDao.getByAnimeId(progress.animeId)
        animeProgressDao.upsert(progress.copy(
            updatedAtEpochMillis = maxOf(progress.updatedAtEpochMillis, (current?.updatedAtEpochMillis ?: 0L) + 1L)
        ))
        syncTrigger.enqueue()
    }

    override suspend fun subscribeToAnime(animeId: String, notificationsEnabled: Boolean, updatedAtEpochMillis: Long) {
        val current = animeSubscriptionDao.getByAnimeId(animeId)
        val timestamp = maxOf(updatedAtEpochMillis, (current?.updatedAtEpochMillis ?: 0L) + 1L)
        animeSubscriptionDao.upsert(
            AnimeSubscriptionEntity(
                animeId = animeId,
                notificationsEnabled = notificationsEnabled,
                updatedAtEpochMillis = timestamp,
                lastKnownEpisodesAired = current?.lastKnownEpisodesAired,
                isSubscribed = true
            )
        )
        syncTrigger.enqueue()
    }

    override suspend fun setSubscriptionNotificationsEnabled(
        animeId: String,
        enabled: Boolean,
        updatedAtEpochMillis: Long
    ) {
        val current = animeSubscriptionDao.getByAnimeId(animeId) ?: return
        if (!current.isSubscribed) return
        val timestamp = maxOf(updatedAtEpochMillis, current.updatedAtEpochMillis + 1L)
        animeSubscriptionDao.upsert(current.copy(
            notificationsEnabled = enabled,
            updatedAtEpochMillis = timestamp
        ))
        syncTrigger.enqueue()
    }

    override suspend fun removeSubscription(animeId: String) {
        val current = animeSubscriptionDao.getByAnimeId(animeId)
        val timestamp = maxOf(System.currentTimeMillis(), (current?.updatedAtEpochMillis ?: 0L) + 1L)
        if (current == null) {
            animeSubscriptionDao.upsert(AnimeSubscriptionEntity(
                animeId = animeId,
                notificationsEnabled = false,
                updatedAtEpochMillis = timestamp,
                isSubscribed = false
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
}
