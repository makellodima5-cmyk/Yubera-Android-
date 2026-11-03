package com.yubera.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yubera.app.core.database.entity.CatalogAnimeEntity
import com.yubera.app.core.database.entity.CatalogMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_anime ORDER BY position ASC")
    fun observeAll(): Flow<List<CatalogAnimeEntity>>

    @Query("SELECT * FROM catalog_anime WHERE animeId = :animeId LIMIT 1")
    fun observeById(animeId: String): Flow<CatalogAnimeEntity?>

    @Query("SELECT * FROM catalog_anime ORDER BY position ASC")
    suspend fun getAll(): List<CatalogAnimeEntity>

    @Query("SELECT * FROM catalog_metadata WHERE id = 0 LIMIT 1")
    suspend fun getMetadata(): CatalogMetadataEntity?

    @Query("DELETE FROM catalog_anime")
    suspend fun clearCatalog()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogAnimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: CatalogMetadataEntity)

    @Transaction
    suspend fun replaceSnapshot(items: List<CatalogAnimeEntity>, metadata: CatalogMetadataEntity) {
        clearCatalog()
        if (items.isNotEmpty()) insertAll(items)
        saveMetadata(metadata)
    }
}
