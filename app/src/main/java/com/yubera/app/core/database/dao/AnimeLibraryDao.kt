package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeLibraryDao {
    @Query("SELECT * FROM anime_library WHERE accountId = :accountId AND isFavorite = 1 ORDER BY updatedAtEpochMillis DESC")
    fun observeFavorites(accountId: String): Flow<List<AnimeLibraryEntity>>

    @Query("SELECT * FROM anime_library WHERE accountId = :accountId AND isInWatchLater = 1 ORDER BY updatedAtEpochMillis DESC")
    fun observeWatchLater(accountId: String): Flow<List<AnimeLibraryEntity>>

    @Query("SELECT * FROM anime_library WHERE accountId = :accountId")
    suspend fun getAll(accountId: String): List<AnimeLibraryEntity>

    @Query("SELECT * FROM anime_library WHERE accountId = :accountId AND animeId = :animeId LIMIT 1")
    suspend fun getByAnimeId(accountId: String, animeId: String): AnimeLibraryEntity?

    @Query("UPDATE anime_library SET accountId = :accountId WHERE accountId = 'legacy-unassigned'")
    suspend fun claimLegacyRows(accountId: String): Int

    @Upsert
    suspend fun upsert(entity: AnimeLibraryEntity)
}
