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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPersonalLibraryRepositoryTest {
    private object NoOpLibrarySyncTrigger : LibrarySyncTrigger {
        override fun enqueue() = Unit
    }

    @Test
    fun favoriteAndWatchLaterAreIndependentAndDoNotCreateSubscription() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), subscriptionDao)

        repository.setFavorite("anime-1", true, 10L)
        repository.setWatchLater("anime-1", true, 20L)

        val saved = libraryDao.getByAnimeId("account-a", "anime-1")
        assertEquals(true, saved?.isFavorite)
        assertEquals(true, saved?.isInWatchLater)
        assertEquals(10L, saved?.favoriteUpdatedAtEpochMillis)
        assertEquals(20L, saved?.watchLaterUpdatedAtEpochMillis)
        assertNull(subscriptionDao.getByAnimeId("account-a", "anime-1"))
    }

    @Test
    fun changingFavoriteDoesNotAdvanceWatchLaterSyncVersion() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), FakeAnimeSubscriptionDao())
        repository.setWatchLater("anime-1", true, 20L)
        repository.setFavorite("anime-1", true, 30L)

        val saved = libraryDao.getByAnimeId("account-a", "anime-1")
        assertEquals(30L, saved?.favoriteUpdatedAtEpochMillis)
        assertEquals(20L, saved?.watchLaterUpdatedAtEpochMillis)
    }

    @Test
    fun explicitSubscriptionDoesNotMutateFavoriteOrWatchLaterLists() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), subscriptionDao)

        repository.subscribeToAnime("anime-2", true, 30L)

        assertNull(libraryDao.getByAnimeId("account-a", "anime-2"))
        assertEquals(true, subscriptionDao.getByAnimeId("account-a", "anime-2")?.notificationsEnabled)
        assertEquals(true, subscriptionDao.getByAnimeId("account-a", "anime-2")?.isSubscribed)
    }

    @Test
    fun reSavingSubscriptionPreservesEpisodeWatermark() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)
        subscriptionDao.upsert(AnimeSubscriptionEntity(
            "anime-5", true, 50L, lastKnownEpisodesAired = 8, accountId = "account-a"
        ))

        repository.subscribeToAnime("anime-5", true, 60L)

        assertEquals(8, subscriptionDao.getByAnimeId("account-a", "anime-5")?.lastKnownEpisodesAired)
    }

    @Test
    fun progressIsStoredSeparatelyFromLibraryMembership() = runTest {
        val libraryDao = FakeAnimeLibraryDao()
        val progressDao = FakeAnimeProgressDao()
        val repository = createRepository(libraryDao, progressDao, FakeAnimeSubscriptionDao())
        val progress = AnimeProgressEntity("anime-3", 1, 4, 125_000L, 1_400_000L, 40L)

        repository.saveProgress(progress)

        assertEquals(progress.copy(accountId = "account-a"), progressDao.getByAnimeId("account-a", "anime-3"))
        assertNull(libraryDao.getByAnimeId("account-a", "anime-3"))
    }

    @Test
    fun disablingSubscriptionNotificationsKeepsSubscriptionRecord() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)

        repository.subscribeToAnime("anime-4", true, 50L)
        repository.setSubscriptionNotificationsEnabled("anime-4", false, 60L)

        assertFalse(subscriptionDao.getByAnimeId("account-a", "anime-4")?.notificationsEnabled ?: true)
        assertTrue(subscriptionDao.getByAnimeId("account-a", "anime-4")?.isSubscribed == true)
    }

    @Test
    fun removingSubscriptionLeavesSyncTombstoneButStopsNotifications() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)
        repository.subscribeToAnime("anime-6", true, 70L)

        repository.removeSubscription("anime-6")

        assertFalse(subscriptionDao.getByAnimeId("account-a", "anime-6")?.isSubscribed ?: true)
        assertTrue(subscriptionDao.getNotificationSubscriptions("account-a").isEmpty())
        assertTrue(subscriptionDao.getAll("account-a").any { it.animeId == "anime-6" && !it.isSubscribed })
    }

    @Test
    fun removingUnknownSubscriptionStillCreatesRemoteDeletionTombstone() = runTest {
        val subscriptionDao = FakeAnimeSubscriptionDao()
        val repository = createRepository(FakeAnimeLibraryDao(), FakeAnimeProgressDao(), subscriptionDao)

        repository.removeSubscription("anime-7")

        assertFalse(subscriptionDao.getByAnimeId("account-a", "anime-7")?.isSubscribed ?: true)
    }

    @Test
    fun personalLibraryIsPartitionedAndRestoredWhenSwitchingAccounts() = runTest {
        val accountStore = FakeActiveAccountStore("account-a")
        val libraryDao = FakeAnimeLibraryDao()
        val repository = createRepository(libraryDao, FakeAnimeProgressDao(), FakeAnimeSubscriptionDao(), accountStore)

        repository.setFavorite("anime-1", true, 10L)
        assertTrue(repository.favorites.first().single().isFavorite)

        accountStore.switchTo("account-b")
        assertTrue(repository.favorites.first().isEmpty())
        repository.setFavorite("anime-1", false, 20L)
        assertFalse(libraryDao.getByAnimeId("account-b", "anime-1")?.isFavorite ?: true)
        assertTrue(libraryDao.getByAnimeId("account-a", "anime-1")?.isFavorite == true)

        accountStore.switchTo("account-a")
        assertTrue(repository.favorites.first().single().isFavorite)
    }

    private fun createRepository(
        libraryDao: AnimeLibraryDao,
        progressDao: AnimeProgressDao,
        subscriptionDao: AnimeSubscriptionDao,
        accountStore: FakeActiveAccountStore = FakeActiveAccountStore("account-a")
    ): PersonalLibraryRepository =
        RoomPersonalLibraryRepository(libraryDao, progressDao, subscriptionDao, NoOpLibrarySyncTrigger, accountStore)

    private class FakeActiveAccountStore(initialAccountId: String) : ActiveAccountStore {
        private val state = MutableStateFlow<String?>(initialAccountId)
        override val activeAccountId: StateFlow<String?> = state
        override fun currentAccountId(): String? = state.value
        fun switchTo(accountId: String?) { state.value = accountId }
        override suspend fun activateAccount(accountId: String, accessToken: String, claimLegacyRows: Boolean) {
            state.value = accountId
        }
        override suspend fun clearActiveAccount() { state.value = null }
    }

    private class FakeAnimeLibraryDao : AnimeLibraryDao {
        private val rows = linkedMapOf<Pair<String, String>, AnimeLibraryEntity>()
        private val state = MutableStateFlow<List<AnimeLibraryEntity>>(emptyList())
        override fun observeFavorites(accountId: String): Flow<List<AnimeLibraryEntity>> =
            state.map { list -> list.filter { it.accountId == accountId && it.isFavorite } }
        override fun observeWatchLater(accountId: String): Flow<List<AnimeLibraryEntity>> =
            state.map { list -> list.filter { it.accountId == accountId && it.isInWatchLater } }
        override suspend fun getAll(accountId: String): List<AnimeLibraryEntity> =
            rows.values.filter { it.accountId == accountId }
        override suspend fun getByAnimeId(accountId: String, animeId: String): AnimeLibraryEntity? =
            rows[accountId to animeId]
        override suspend fun claimLegacyRows(accountId: String): Int {
            val legacy = rows.values.filter { it.accountId == ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID }
            legacy.forEach { row ->
                rows.remove(row.accountId to row.animeId)
                rows[accountId to row.animeId] = row.copy(accountId = accountId)
            }
            state.value = rows.values.toList()
            return legacy.size
        }
        override suspend fun upsert(entity: AnimeLibraryEntity) {
            rows[entity.accountId to entity.animeId] = entity
            state.value = rows.values.toList()
        }
    }

    private class FakeAnimeProgressDao : AnimeProgressDao {
        private val rows = linkedMapOf<Pair<String, String>, AnimeProgressEntity>()
        private val state = MutableStateFlow<List<AnimeProgressEntity>>(emptyList())
        override fun observeAll(accountId: String): Flow<List<AnimeProgressEntity>> =
            state.map { list -> list.filter { it.accountId == accountId } }
        override suspend fun getAll(accountId: String): List<AnimeProgressEntity> =
            rows.values.filter { it.accountId == accountId }
        override suspend fun getByAnimeId(accountId: String, animeId: String): AnimeProgressEntity? =
            rows[accountId to animeId]
        override suspend fun claimLegacyRows(accountId: String): Int {
            val legacy = rows.values.filter { it.accountId == ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID }
            legacy.forEach { row ->
                rows.remove(row.accountId to row.animeId)
                rows[accountId to row.animeId] = row.copy(accountId = accountId)
            }
            state.value = rows.values.toList()
            return legacy.size
        }
        override suspend fun upsert(entity: AnimeProgressEntity) {
            rows[entity.accountId to entity.animeId] = entity
            state.value = rows.values.toList()
        }
        override suspend fun deleteByAnimeId(accountId: String, animeId: String) {
            rows.remove(accountId to animeId)
            state.value = rows.values.toList()
        }
    }

    private class FakeAnimeSubscriptionDao : AnimeSubscriptionDao {
        private val rows = linkedMapOf<Pair<String, String>, AnimeSubscriptionEntity>()
        private val state = MutableStateFlow<List<AnimeSubscriptionEntity>>(emptyList())
        override fun observeAll(accountId: String): Flow<List<AnimeSubscriptionEntity>> =
            state.map { list -> list.filter { it.accountId == accountId && it.isSubscribed } }
        override suspend fun getAll(accountId: String): List<AnimeSubscriptionEntity> =
            rows.values.filter { it.accountId == accountId }
        override suspend fun getByAnimeId(accountId: String, animeId: String): AnimeSubscriptionEntity? =
            rows[accountId to animeId]
        override suspend fun getNotificationSubscriptions(accountId: String): List<AnimeSubscriptionEntity> =
            rows.values.filter { it.accountId == accountId && it.isSubscribed && it.notificationsEnabled }
        override suspend fun updateEpisodeWatermark(accountId: String, animeId: String, episodesAired: Int) {
            rows[accountId to animeId]?.let { rows[accountId to animeId] = it.copy(lastKnownEpisodesAired = episodesAired) }
            state.value = rows.values.toList()
        }
        override suspend fun claimLegacyRows(accountId: String): Int {
            val legacy = rows.values.filter { it.accountId == ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID }
            legacy.forEach { row ->
                rows.remove(row.accountId to row.animeId)
                rows[accountId to row.animeId] = row.copy(accountId = accountId)
            }
            state.value = rows.values.toList()
            return legacy.size
        }
        override suspend fun upsert(entity: AnimeSubscriptionEntity) {
            rows[entity.accountId to entity.animeId] = entity
            state.value = rows.values.toList()
        }
        override suspend fun deleteByAnimeId(accountId: String, animeId: String) {
            rows.remove(accountId to animeId)
            state.value = rows.values.toList()
        }
    }
}
