package com.yubera.app.data.library

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yubera.app.core.database.YuberaDatabase
import com.yubera.app.core.database.dao.AnimeLibraryDao
import com.yubera.app.core.database.dao.AnimeProgressDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.entity.AnimeLibraryEntity
import com.yubera.app.core.database.entity.AnimeProgressEntity
import com.yubera.app.core.database.entity.AnimeSubscriptionEntity
import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.core.session.ActiveAccountStore
import com.yubera.app.features.library.LibrarySyncTrigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomPersonalLibraryRepositoryTest {
    private lateinit var database: YuberaDatabase
    private lateinit var accountStore: FakeActiveAccountStore
    private lateinit var repository: PersonalLibraryRepository

    private object NoOpLibrarySyncTrigger : LibrarySyncTrigger {
        override fun enqueue() = Unit
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YuberaDatabase::class.java
        ).allowMainThreadQueries().build()
        accountStore = FakeActiveAccountStore("account-a")
        repository = RoomPersonalLibraryRepository(
            database,
            database.animeLibraryDao(),
            database.animeProgressDao(),
            database.animeSubscriptionDao(),
            NoOpLibrarySyncTrigger,
            accountStore
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun favoriteAndWatchLaterAreIndependentAndDoNotCreateSubscription() = runTest {
        repository.setFavorite("anime-1", true, 10L)
        repository.setWatchLater("anime-1", true, 20L)

        val saved = database.animeLibraryDao().getByAnimeId("account-a", "anime-1")
        assertEquals(true, saved?.isFavorite)
        assertEquals(true, saved?.isInWatchLater)
        assertTrue(database.animeSubscriptionDao().getAll("account-a").isEmpty())
    }

    @Test
    fun changingFavoriteDoesNotAdvanceWatchLaterSyncVersion() = runTest {
        repository.setWatchLater("anime-1", true, 20L)
        repository.setFavorite("anime-1", true, 30L)

        val saved = database.animeLibraryDao().getByAnimeId("account-a", "anime-1")
        assertEquals(30L, saved?.favoriteUpdatedAtEpochMillis)
        assertEquals(20L, saved?.watchLaterUpdatedAtEpochMillis)
    }

    @Test
    fun explicitSubscriptionDoesNotMutateFavoriteOrWatchLaterLists() = runTest {
        repository.subscribeToAnime("anime-2", true, 30L)
        assertNull(database.animeLibraryDao().getByAnimeId("account-a", "anime-2"))
        assertEquals(true, database.animeSubscriptionDao().getByAnimeId("account-a", "anime-2")?.isSubscribed)
    }

    @Test
    fun reSavingSubscriptionPreservesEpisodeWatermark() = runTest {
        database.animeSubscriptionDao().upsert(
            AnimeSubscriptionEntity("anime-5", true, 50L, lastKnownEpisodesAired = 8, accountId = "account-a")
        )
        repository.subscribeToAnime("anime-5", true, 60L)
        assertEquals(8, database.animeSubscriptionDao().getByAnimeId("account-a", "anime-5")?.lastKnownEpisodesAired)
    }

    @Test
    fun progressIsStoredSeparatelyFromLibraryMembership() = runTest {
        val progress = AnimeProgressEntity("anime-3", 1, 4, 125_000L, 1_400_000L, 40L)
        repository.saveProgress(progress)
        assertEquals(progress.copy(accountId = "account-a"), database.animeProgressDao().getByAnimeId("account-a", "anime-3"))
        assertNull(database.animeLibraryDao().getByAnimeId("account-a", "anime-3"))
    }

    @Test
    fun disablingSubscriptionNotificationsKeepsSubscriptionRecord() = runTest {
        repository.subscribeToAnime("anime-4", true, 50L)
        repository.setSubscriptionNotificationsEnabled("anime-4", false, 60L)
        val subscription = database.animeSubscriptionDao().getByAnimeId("account-a", "anime-4")
        assertFalse(subscription?.notificationsEnabled ?: true)
        assertTrue(subscription?.isSubscribed == true)
    }

    @Test
    fun removingSubscriptionLeavesSyncTombstoneButStopsNotifications() = runTest {
        repository.subscribeToAnime("anime-6", true, 70L)
        repository.removeSubscription("anime-6")
        assertFalse(database.animeSubscriptionDao().getByAnimeId("account-a", "anime-6")?.isSubscribed ?: true)
        assertTrue(database.animeSubscriptionDao().getNotificationSubscriptions("account-a").isEmpty())
        assertTrue(database.animeSubscriptionDao().getAll("account-a").any { it.animeId == "anime-6" && !it.isSubscribed })
    }

    @Test
    fun removingUnknownSubscriptionStillCreatesRemoteDeletionTombstone() = runTest {
        repository.removeSubscription("anime-7")
        assertFalse(database.animeSubscriptionDao().getByAnimeId("account-a", "anime-7")?.isSubscribed ?: true)
    }

    @Test
    fun personalLibraryIsPartitionedAndRestoredWhenSwitchingAccounts() = runTest {
        repository.setFavorite("anime-1", true, 10L)
        assertTrue(repository.favorites.first().single().isFavorite)

        accountStore.switchTo("account-b")
        assertTrue(repository.favorites.first().isEmpty())
        repository.setFavorite("anime-1", false, 20L)
        assertFalse(database.animeLibraryDao().getByAnimeId("account-b", "anime-1")?.isFavorite ?: true)
        assertTrue(database.animeLibraryDao().getByAnimeId("account-a", "anime-1")?.isFavorite == true)

        accountStore.switchTo("account-a")
        assertTrue(repository.favorites.first().single().isFavorite)
    }

    @Test
    fun concurrentLibraryEditsAreSerializedAndRetainBothFields() = runTest {
        kotlinx.coroutines.coroutineScope {
            val favorite = kotlinx.coroutines.async { repository.setFavorite("anime-8", true, 100L) }
            val watchLater = kotlinx.coroutines.async { repository.setWatchLater("anime-8", true, 100L) }
            favorite.await()
            watchLater.await()
        }
        val saved = database.animeLibraryDao().getByAnimeId("account-a", "anime-8")
        assertTrue(saved?.isFavorite == true)
        assertTrue(saved?.isInWatchLater == true)
    }

    @Test
    fun accountSwitchDuringQueuedMutationDoesNotWriteIntoPreviousAccount() = runTest {
        // Switch before the mutation starts; it must use the new active account.
        accountStore.switchTo("account-b")
        repository.setFavorite("anime-9", true, 200L)
        assertNull(database.animeLibraryDao().getByAnimeId("account-a", "anime-9"))
        assertTrue(database.animeLibraryDao().getByAnimeId("account-b", "anime-9")?.isFavorite == true)
    }

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
}
