package com.yubera.app.data.library

import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPersonalLibraryRepositoryTest {

    @Test
    fun favoriteAndWatchLaterAreIndependentAndDoNotCreateSubscription() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), subscriptionDao)

        repository.setFavorite(animeId = "anime-1", isFavorite = true, updatedAtEpochMillis = 10L)
        repository.setWatchLater(animeId = "anime-1", isInWatchLater = true, updatedAtEpochMillis = 20L)

        val saved = libraryDao.getByAnimeId("anime-1")
        assertEquals(true, saved?.isFavorite)
        assertEquals(true, saved?.isInWatchLater)
        assertNull(subscriptionDao.getByAnimeId("anime-1"))
    }

    @Test
    fun explicitSubscriptionDoesNotMutateFavoriteOrWatchLaterLists() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), subscriptionDao)

        repository.subscribeToAnime(
            animeId = "anime-2",
            notificationsEnabled = true,
            updatedAtEpochMillis = 30L
        )

        assertNull(libraryDao.getByAnimeId("anime-2"))
        assertEquals(true, subscriptionDao.getByAnimeId("anime-2")?.notificationsEnabled)
    }

    @Test
    fun progressIsStoredSeparatelyFromLibraryMembership() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val progressDao = FakeAnimeProgressDao()
        val repository = createRepository(libraryDao, progressDao, FakeAnimeSubscriptionDao())
        val progress = AnimeProgressEntity(
            animeId = "anime-3",
            seasonNumber = 1,
            episodeNumber = 4,
            positionMillis = 125_000L,
            durationMillis = 1_400_000L,
            updatedAtEpochMillis = 40L
        )

        repository.saveProgress(progress)

        assertEquals(progress, progressDao.getByAnimeId("anime-3"))
        assertNull(libraryDao.getByAnimeId("anime-3"))
    }

    @Test
    fun disablingSubscriptionNotificationsKeepsSubscriptionRecord() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)

        repository.subscribeToAnime("anime-4", notificationsEnabled = true, updatedAtEpochMillis = 50L)
        repository.setSubscriptionNotificationsEnabled("anime-4", enabled = false, updatedAtEpochMillis = 60L)

        assertFalse(subscriptionDao.getByAnimeId("anime-4")?.notificationsEnabled ?: true)
        assertTrue(subscriptionDao.getByAnimeId("anime-4") != null)
    }

    private fun createRepository(
        libraryDao: AnimeLibraryDao,
        progressDao: AnimeProgressDao,
        subscriptionDao: AnimeSubscriptionDao
    ): PersonalLibraryRepository = RoomPersonalLibraryRepository(
        animeLibraryDao = libraryDao,
        animeProgressDao = progressDao,
        animeSubscriptionDao = subscriptionDao
    )

    private class FakeAnimeLibraryDao : AnimeLibraryDao {
        private val rows = linkedMapOf<String, AnimeLibraryEntity>()
        private val state = MutableStateFlow<List<AnimeLibraryEntity>>(emptyList())

        override fun observeFavorites(): Flow<List<AnimeLibraryEntity>> =
            state.map { entries -> entries.filter(AnimeLibraryEntity::isFavorite) }

        override fun observeWatchLater(): Flow<List<AnimeLibraryEntity>> =
            state.map { entries -> entries.filter(AnimeLibraryEntity::isInWatchLater) }

        override suspend fun getByAnimeId(animeId: String): AnimeLibraryEntity? = rows[animeId]

        override suspend fun upsert(entity: AnimeLibraryEntity) {
            rows[entity.animeId] = entity
            state.value = rows.values.toList()
        }
    }

    private class FakeAnimeProgressDao : AnimeProgressDao {
        private val rows = linkedMapOf<String, AnimeProgressEntity>()
        private val state = MutableStateFlow<List<AnimeProgressEntity>>(emptyList())

        override fun observeAll(): Flow<List<AnimeProgressEntity>> = state

        override suspend fun getByAnimeId(animeId: String): AnimeProgressEntity? = rows[animeId]

        override suspend fun upsert(entity: AnimeProgressEntity) {
            rows[entity.animeId] = entity
            state.value = rows.values.toList()
        }
    }

    private class FakeAnimeSubscriptionDao : AnimeSubscriptionDao {
        private val rows = linkedMapOf<String, AnimeSubscriptionEntity>()
        private val state = MutableStateFlow<List<AnimeSubscriptionEntity>>(emptyList())

        override fun observeAll(): Flow<List<AnimeSubscriptionEntity>> = state

        override suspend fun getByAnimeId(animeId: String): AnimeSubscriptionEntity? = rows[animeId]

        override suspend fun upsert(entity: AnimeSubscriptionEntity) {
            rows[entity.animeId] = entity
            state.value = rows.values.toList()
        }

        override suspend fun deleteByAnimeId(animeId: String) {
            rows.remove(animeId)
            state.value = rows.values.toList()
        }
    }
}
