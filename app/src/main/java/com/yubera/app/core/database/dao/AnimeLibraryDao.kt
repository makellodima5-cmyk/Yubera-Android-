package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeLibraryDao {
    @Query("SELECT * FROM anime_library WHERE isFavorite = 1 ORDER BY updatedAtEpochMillis DESC")
    fun observeFavorites(): Flow<List<AnimeLibraryEntity>>

    @Query("SELECT * FROM anime_library WHERE isInWatchLater = 1 ORDER BY updatedAtEpochMillis DESC")
    fun observeWatchLater(): Flow<List<AnimeLibraryEntity>>

    @Query("SELECT * FROM anime_library")
    suspend fun getAll(): List<AnimeLibraryEntity>

    @Query("SELECT * FROM anime_library WHERE animeId = :animeId LIMIT 1")
    suspend fun getByAnimeId(animeId: String): AnimeLibraryEntity?

    @Upsert
    suspend fun upsert(entity: AnimeLibraryEntity)
}
