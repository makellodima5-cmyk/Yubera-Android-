package com.yubera.app.features.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yubera.app.MainActivity
import com.yubera.app.core.database.dao.AnimeEpisodeNotificationDao
import com.yubera.app.core.database.dao.AnimeSubscriptionDao
import com.yubera.app.core.database.dao.CatalogDao
import com.yubera.app.core.database.entity.AnimeEpisodeNotificationEntity
import com.yubera.app.core.session.ActiveAccountStore
import com.yubera.app.core.session.SecureTokenStore
import com.yubera.app.data.news.ShikimoriEpisodeUpdatesApi
import com.yubera.app.data.news.ShikimoriGraphQlRequest
import com.yubera.app.data.settings.UserSettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EpisodeNotificationWorkerEntryPoint {
    fun animeSubscriptionDao(): AnimeSubscriptionDao
    fun animeEpisodeNotificationDao(): AnimeEpisodeNotificationDao
    fun catalogDao(): CatalogDao
    fun shikimoriEpisodeUpdatesApi(): ShikimoriEpisodeUpdatesApi
    fun userSettingsRepository(): UserSettingsRepository
    fun secureTokenStore(): SecureTokenStore
    fun activeAccountStore(): ActiveAccountStore
}

private class GlobalNotificationsDisabledException : Exception()
private class ActiveAccountChangedException : Exception()

class EpisodeNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, EpisodeNotificationWorkerEntryPoint::class.java)
        val tokenStore = entry.secureTokenStore()
        val activeAccountStore = entry.activeAccountStore()
        val token = tokenStore.getAccessToken() ?: return Result.success()
        val accountId = tokenStore.getAccountId()
            ?.takeIf { it.isNotBlank() && it != ActiveAccountStore.LEGACY_UNASSIGNED_ACCOUNT_ID }
            ?: return Result.success()
        if (!sessionMatches(tokenStore, activeAccountStore, token, accountId)) return Result.success()

        return try {
            val settingsRepository = entry.userSettingsRepository()
            val settings = settingsRepository.settings.first()
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createChannel(manager)
            if (!EpisodeNotificationPolicy.shouldDeliverNotification(
                    notificationsEnabled = settings.notificationsEnabled,
                    newEpisodeNotificationsEnabled = settings.newEpisodeNotificationsEnabled,
                    subscriptionNotificationsEnabled = true,
                    systemNotificationsEnabled = hasPermission() && notificationsAvailable(manager)
                )
            ) return Result.success()

            val subscriptionDao = entry.animeSubscriptionDao()
            val subscriptions = subscriptionDao.getNotificationSubscriptions(accountId)
            if (subscriptions.isEmpty()) return Result.success()

            val titles = entry.catalogDao().getAll().associate { it.animeId to it.title }
            val notificationDao = entry.animeEpisodeNotificationDao()
            val api = entry.shikimoriEpisodeUpdatesApi()
            val batches = subscriptions.chunked(MAX_IDS_PER_QUERY)

            batches.forEachIndexed { batchIndex, batch ->
                ensureCurrentAccount(tokenStore, activeAccountStore, token, accountId)
                val ids = batch.mapNotNull { it.animeId.toLongOrNull()?.takeIf { value -> value > 0 } }
                if (ids.isNotEmpty()) {
                    val idList = ids.joinToString(",")
                    val query = "{ animes(ids: \"" + idList + "\", limit: " + ids.size + ") { id name russian episodesAired } }"
                    val response = api.getAnimeUpdates(ShikimoriGraphQlRequest(query))
                    if (response.errors.isNotEmpty()) throw IllegalStateException(response.errors.joinToString { it.message })
                    ensureCurrentAccount(tokenStore, activeAccountStore, token, accountId)
                    val updates = response.data?.animes.orEmpty().associateBy { it.id }

                    batch.forEach { subscription ->
                        ensureCurrentAccount(tokenStore, activeAccountStore, token, accountId)
                        val update = updates[subscription.animeId] ?: return@forEach
                        val count = update.episodesAired?.coerceAtLeast(0) ?: return@forEach
                        val previous = subscription.lastKnownEpisodesAired

                        EpisodeNotificationPolicy.releasedEpisodes(previous, count).forEach { episode ->
                            ensureCurrentAccount(tokenStore, activeAccountStore, token, accountId)
                            notificationDao.insertIfNew(AnimeEpisodeNotificationEntity(
                                animeId = subscription.animeId,
                                episodeNumber = episode,
                                createdAtEpochMillis = System.currentTimeMillis(),
                                accountId = accountId
                            ))
                            val saved = notificationDao.getByEpisode(accountId, subscription.animeId, episode)
                            if (saved?.deliveredAtEpochMillis == null) {
                                val currentSubscription = subscriptionDao.getByAnimeId(accountId, subscription.animeId)
                                if (currentSubscription?.notificationsEnabled == true && currentSubscription.isSubscribed) {
                                    val currentSettings = settingsRepository.settings.first()
                                    val canDeliver = EpisodeNotificationPolicy.shouldDeliverNotification(
                                        notificationsEnabled = currentSettings.notificationsEnabled,
                                        newEpisodeNotificationsEnabled = currentSettings.newEpisodeNotificationsEnabled,
                                        subscriptionNotificationsEnabled = currentSubscription.notificationsEnabled,
                                        systemNotificationsEnabled = hasPermission() && notificationsAvailable(manager)
                                    )
                                    if (!canDeliver) {
                                        if (!currentSettings.notificationsEnabled || !currentSettings.newEpisodeNotificationsEnabled) {
                                            throw GlobalNotificationsDisabledException()
                                        }
                                        throw IllegalStateException("Notifications became unavailable during delivery")
                                    }
                                    ensureCurrentAccount(tokenStore, activeAccountStore, token, accountId)
                                    val title = titles[subscription.animeId]
                                        ?: update.displayName.ifBlank { "Аниме " + subscription.animeId }
                                    showNotification(stableId(accountId, subscription.animeId, episode), title, episode)
                                    notificationDao.markDelivered(accountId, subscription.animeId, episode, System.currentTimeMillis())
                                }
                            }
                        }

                        ensureCurrentAccount(tokenStore, activeAccountStore, token, accountId)
                        val watermark = EpisodeNotificationPolicy.nextWatermark(previous, count) ?: return@forEach
                        subscriptionDao.updateEpisodeWatermark(accountId, subscription.animeId, watermark)
                    }
                }
                if (batchIndex < batches.lastIndex) delay(REQUEST_SPACING_MILLIS)
            }
            Result.success()
        } catch (_: GlobalNotificationsDisabledException) {
            Result.success()
        } catch (_: ActiveAccountChangedException) {
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun sessionMatches(
        tokenStore: SecureTokenStore,
        activeAccountStore: ActiveAccountStore,
        token: String,
        accountId: String
    ): Boolean =
        tokenStore.getAccessToken() == token &&
            tokenStore.getAccountId() == accountId &&
            activeAccountStore.currentAccountId() == accountId

    private fun ensureCurrentAccount(
        tokenStore: SecureTokenStore,
        activeAccountStore: ActiveAccountStore,
        token: String,
        accountId: String
    ) {
        if (!sessionMatches(tokenStore, activeAccountStore, token, accountId)) {
            throw ActiveAccountChangedException()
        }
    }

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun createChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Новые серии", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Новые эпизоды аниме из ваших подписок"
                }
            )
        }
    }

    private fun notificationsAvailable(manager: NotificationManager): Boolean {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun showNotification(id: Int, title: String, episode: Int) {
        // Permission can be revoked between the worker's initial check and delivery.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val intent = android.content.Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Вышла новая серия: " + episode)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        } catch (_: SecurityException) {
            // Notification permission may be revoked while this notification is posted.
        }
    }

    private fun stableId(accountId: String, animeId: String, episode: Int): Int = 31 * (31 * accountId.hashCode() + animeId.hashCode()) + episode

    companion object {
        const val CHANNEL_ID = "yubera_new_episodes"
        const val MAX_IDS_PER_QUERY = 50
        const val REQUEST_SPACING_MILLIS = 700L
    }
}
