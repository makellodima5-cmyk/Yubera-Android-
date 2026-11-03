package com.yubera.app.core.domain.library

import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import kotlinx.coroutines.flow.Flow

interface PersonalLibraryRepository {
    val favorites: Flow<List<AnimeLibraryEntity>>
    val watchLater: Flow<List<AnimeLibraryEntity>>
    val progress: Flow<List<AnimeProgressEntity>>
    val subscriptions: Flow<List<AnimeSubscriptionEntity>>

    suspend fun setFavorite(animeId: String, isFavorite: Boolean, updatedAtEpochMillis: Long)
    suspend fun setWatchLater(animeId: String, isInWatchLater: Boolean, updatedAtEpochMillis: Long)
    suspend fun saveProgress(progress: AnimeProgressEntity)
    suspend fun subscribeToAnime(animeId: String, notificationsEnabled: Boolean, updatedAtEpochMillis: Long)
    suspend fun setSubscriptionNotificationsEnabled(animeId: String, enabled: Boolean, updatedAtEpochMillis: Long)
    suspend fun removeSubscription(animeId: String)
}
