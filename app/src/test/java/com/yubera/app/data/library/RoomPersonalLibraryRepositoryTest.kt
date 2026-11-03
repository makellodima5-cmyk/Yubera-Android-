package com.yubera.app.data.library

import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.features.library.LibrarySyncTrigger
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
    private object NoOpLibrarySyncTrigger : LibrarySyncTrigger { override fun enqueue() = Unit }
    @Test
    fun favoriteAndWatchLaterAreIndependentAndDoNotCreateSubscription() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), subscriptionDao)

        repository.setFavorite("anime-1", true, 10L)
        repository.setWatchLater("anime-1", true, 20L)

        val saved = libraryDao.getByAnimeId("anime-1")
        assertEquals(true, saved?.isFavorite)
        assertEquals(true, saved?.isInWatchLater)
        assertEquals(10L, saved?.favoriteUpdatedAtEpochMillis)
        assertEquals(20L, saved?.watchLaterUpdatedAtEpochMillis)
        assertNull(subscriptionDao.getByAnimeId("anime-1"))
    }

    @Test
    fun changingFavoriteDoesNotAdvanceWatchLaterSyncVersion() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), FakeAnimeSubscriptionDao())
        repository.setWatchLater("anime-1", true, 20L)
        repository.setFavorite("anime-1", true, 30L)

        val saved = libraryDao.getByAnimeId("anime-1")
        assertEquals(30L, saved?.favoriteUpdatedAtEpochMillis)
        assertEquals(20L, saved?.watchLaterUpdatedAtEpochMillis)
    }

    @Test
    fun explicitSubscriptionDoesNotMutateFavoriteOrWatchLaterLists() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), subscriptionDao)

        repository.subscribeToAnime("anime-2", true, 30L)

        assertNull(libraryDao.getByAnimeId("anime-2"))
        assertEquals(true, subscriptionDao.getByAnimeId("anime-2")?.notificationsEnabled)
        assertEquals(true, subscriptionDao.getByAnimeId("anime-2")?.isSubscribed)
    }

    @Test
    fun reSavingSubscriptionPreservesEpisodeWatermark() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)
        subscriptionDao.upsert(AnimeSubscriptionEntity("anime-5", true, 50L, lastKnownEpisodesAired = 8))

        repository.subscribeToAnime("anime-5", true, 60L)

        assertEquals(8, subscriptionDao.getByAnimeId("anime-5")?.lastKnownEpisodesAired)
    }

    @Test
    fun progressIsStoredSeparatelyFromLibraryMembership() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val progressDao = FakeAnimeProgressDao()
        val repository = createRepository(libraryDao, progressDao, FakeAnimeSubscriptionDao())
        val progress = AnimeProgressEntity("anime-3", 1, 4, 125_000L, 1_400_000L, 40L)

        repository.saveProgress(progress)

        assertEquals(progress, progressDao.getByAnimeId("anime-3"))
        assertNull(libraryDao.getByAnimeId("anime-3"))
    }

    @Test
    fun disablingSubscriptionNotificationsKeepsSubscriptionRecord() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)

        repository.subscribeToAnime("anime-4", true, 50L)
        repository.setSubscriptionNotificationsEnabled("anime-4", false, 60L)

        assertFalse(subscriptionDao.getByAnimeId("anime-4")?.notificationsEnabled ?: true)
        assertTrue(subscriptionDao.getByAnimeId("anime-4")?.isSubscribed == true)
    }

    @Test
    fun removingSubscriptionLeavesSyncTombstoneButStopsNotifications() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)
        repository.subscribeToAnime("anime-6", true, 70L)

        repository.removeSubscription("anime-6")

        assertFalse(subscriptionDao.getByAnimeId("anime-6")?.isSubscribed ?: true)
        assertTrue(subscriptionDao.getNotificationSubscriptions().isEmpty())
        assertTrue(subscriptionDao.getAll().any { it.animeId == "anime-6" && !it.isSubscribed })
    }

    @Test
    fun removingUnknownSubscriptionStillCreatesRemoteDeletionTombstone() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)

        repository.removeSubscription("anime-7")

        assertFalse(subscriptionDao.getByAnimeId("anime-7")?.isSubscribed ?: true)
    }

    private fun createRepository(
        libraryDao: AnimeLibraryDao,
        progressDao: AnimeProgressDao,
        subscriptionDao: AnimeSubscriptionDao
    ): PersonalLibraryRepository = RoomPersonalLibraryRepository(libraryDao, progressDao, subscriptionDao, NoOpLibrarySyncTrigger)

    private class FakeAnimeLibraryDao : AnimeLibraryDao {
        private val rows = linkedMapOf<String, AnimeLibraryEntity>()
        private val state = MutableStateFlow<List<AnimeLibraryEntity>>(emptyList())
        override fun observeFavorites(): Flow<List<AnimeLibraryEntity>> = state.map { it.filter(AnimeLibraryEntity::isFavorite) }
        override fun observeWatchLater(): Flow<List<AnimeLibraryEntity>> = state.map { it.filter(AnimeLibraryEntity::isInWatchLater) }
        override suspend fun getAll(): List<AnimeLibraryEntity> = rows.values.toList()
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
        override suspend fun getAll(): List<AnimeProgressEntity> = rows.values.toList()
        override suspend fun getByAnimeId(animeId: String): AnimeProgressEntity? = rows[animeId]
        override suspend fun upsert(entity: AnimeProgressEntity) {
            rows[entity.animeId] = entity
            state.value = rows.values.toList()
        }
    }

    private class FakeAnimeSubscriptionDao : AnimeSubscriptionDao {
        private val rows = linkedMapOf<String, AnimeSubscriptionEntity>()
        private val state = MutableStateFlow<List<AnimeSubscriptionEntity>>(emptyList())
        override fun observeAll(): Flow<List<AnimeSubscriptionEntity>> = state.map { list -> list.filter(AnimeSubscriptionEntity::isSubscribed) }
        override suspend fun getAll(): List<AnimeSubscriptionEntity> = rows.values.toList()
        override suspend fun getByAnimeId(animeId: String): AnimeSubscriptionEntity? = rows[animeId]
        override suspend fun getNotificationSubscriptions(): List<AnimeSubscriptionEntity> = rows.values.filter { it.isSubscribed && it.notificationsEnabled }
        override suspend fun updateEpisodeWatermark(animeId: String, episodesAired: Int) {
            rows[animeId]?.let { rows[animeId] = it.copy(lastKnownEpisodesAired = episodesAired) }
        }
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
