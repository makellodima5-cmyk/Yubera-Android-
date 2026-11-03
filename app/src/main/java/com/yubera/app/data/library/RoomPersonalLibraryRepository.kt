package com.yubera.app.data.library

import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RoomPersonalLibraryRepository @Inject constructor(
    private val animeLibraryDao: AnimeLibraryDao,
    private val animeProgressDao: AnimeProgressDao,
    private val animeSubscriptionDao: AnimeSubscriptionDao
) : PersonalLibraryRepository {

    override val favorites: Flow<List<AnimeLibraryEntity>> = animeLibraryDao.observeFavorites()
    override val watchLater: Flow<List<AnimeLibraryEntity>> = animeLibraryDao.observeWatchLater()
    override val progress: Flow<List<AnimeProgressEntity>> = animeProgressDao.observeAll()
    override val subscriptions: Flow<List<AnimeSubscriptionEntity>> = animeSubscriptionDao.observeAll()

    override suspend fun setFavorite(animeId: String, isFavorite: Boolean, updatedAtEpochMillis: Long) {
        val current = animeLibraryDao.getByAnimeId(animeId)
        animeLibraryDao.upsert(
            AnimeLibraryEntity(
                animeId = animeId,
                isFavorite = isFavorite,
                isInWatchLater = current?.isInWatchLater ?: false,
                updatedAtEpochMillis = updatedAtEpochMillis
            )
        )
    }

    override suspend fun setWatchLater(animeId: String, isInWatchLater: Boolean, updatedAtEpochMillis: Long) {
        val current = animeLibraryDao.getByAnimeId(animeId)
        animeLibraryDao.upsert(
            AnimeLibraryEntity(
                animeId = animeId,
                isFavorite = current?.isFavorite ?: false,
                isInWatchLater = isInWatchLater,
                updatedAtEpochMillis = updatedAtEpochMillis
            )
        )
    }

    override suspend fun saveProgress(progress: AnimeProgressEntity) {
        animeProgressDao.upsert(progress)
    }

    override suspend fun subscribeToAnime(animeId: String, notificationsEnabled: Boolean, updatedAtEpochMillis: Long) {
        val current = animeSubscriptionDao.getByAnimeId(animeId)
        animeSubscriptionDao.upsert(
            AnimeSubscriptionEntity(animeId, notificationsEnabled, updatedAtEpochMillis, current?.lastKnownEpisodesAired)
        )
    }

    override suspend fun setSubscriptionNotificationsEnabled(
        animeId: String,
        enabled: Boolean,
        updatedAtEpochMillis: Long
    ) {
        val current = animeSubscriptionDao.getByAnimeId(animeId) ?: return
        animeSubscriptionDao.upsert(current.copy(
            notificationsEnabled = enabled,
            updatedAtEpochMillis = updatedAtEpochMillis
        ))
    }

    override suspend fun removeSubscription(animeId: String) {
        animeSubscriptionDao.deleteByAnimeId(animeId)
    }
}
