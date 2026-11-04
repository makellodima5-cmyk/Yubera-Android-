package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yubera.app.core.database.entity.AnimeEpisodeNotificationEntity

@Dao
interface AnimeEpisodeNotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(entity: AnimeEpisodeNotificationEntity): Long

    @Query("SELECT * FROM anime_episode_notifications WHERE accountId = :accountId AND animeId = :animeId AND episodeNumber = :episodeNumber LIMIT 1")
    suspend fun getByEpisode(accountId: String, animeId: String, episodeNumber: Int): AnimeEpisodeNotificationEntity?

    @Query("UPDATE anime_episode_notifications SET deliveredAtEpochMillis = :deliveredAt WHERE accountId = :accountId AND animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun markDelivered(accountId: String, animeId: String, episodeNumber: Int, deliveredAt: Long)

    @Query("UPDATE anime_episode_notifications SET accountId = :accountId WHERE accountId = 'legacy-unassigned'")
    suspend fun claimLegacyRows(accountId: String): Int
}
