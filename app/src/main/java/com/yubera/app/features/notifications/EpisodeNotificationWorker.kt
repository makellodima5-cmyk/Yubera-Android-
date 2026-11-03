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
}
class EpisodeNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
 override suspend fun doWork(): Result {
  val entry = EntryPointAccessors.fromApplication(applicationContext, EpisodeNotificationWorkerEntryPoint::class.java)
  return try {
   val settings = entry.userSettingsRepository().settings.first()
   if (!settings.notificationsEnabled || !settings.newEpisodeNotificationsEnabled || !hasPermission()) return Result.success()
   val subscriptions = entry.animeSubscriptionDao().getNotificationSubscriptions()
   if (subscriptions.isEmpty()) return Result.success()
   val titles = entry.catalogDao().getAll().associate { it.animeId to it.title }
   val subscriptionDao = entry.animeSubscriptionDao()
   val notificationDao = entry.animeEpisodeNotificationDao()
   val api = entry.shikimoriEpisodeUpdatesApi()
   val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
   createChannel(manager)
   val batches = subscriptions.chunked(MAX_IDS_PER_QUERY)
   batches.forEachIndexed { batchIndex, batch ->
    val ids = batch.mapNotNull { it.animeId.toLongOrNull()?.takeIf { value -> value > 0 } }
    if (ids.isNotEmpty()) {
     val idList = ids.joinToString(",")
     val query = "{ animes(ids: \"" + idList + "\", limit: " + ids.size + ") { id name russian episodesAired } }"
     val response = api.getAnimeUpdates(ShikimoriGraphQlRequest(query))
     if (response.errors.isNotEmpty()) throw IllegalStateException(response.errors.joinToString { it.message })
     val updates = response.data?.animes.orEmpty().associateBy { it.id }
     batch.forEach { subscription ->
      val update = updates[subscription.animeId] ?: return@forEach
      val count = update.episodesAired?.coerceAtLeast(0) ?: return@forEach
      val previous = subscription.lastKnownEpisodesAired
      EpisodeNotificationPolicy.releasedEpisodes(previous, count).forEach { episode ->
       notificationDao.insertIfNew(AnimeEpisodeNotificationEntity(subscription.animeId, episode, System.currentTimeMillis()))
       val saved = notificationDao.getByEpisode(subscription.animeId, episode)
       if (saved?.deliveredAtEpochMillis == null) {
        val title = titles[subscription.animeId] ?: update.displayName.ifBlank { "Аниме " + subscription.animeId }
        showNotification(stableId(subscription.animeId, episode), title, episode)
        notificationDao.markDelivered(subscription.animeId, episode, System.currentTimeMillis())
       }
      }
      val watermark = EpisodeNotificationPolicy.nextWatermark(previous, count) ?: return@forEach
      subscriptionDao.updateEpisodeWatermark(subscription.animeId, watermark)
     }
    }
    if (batchIndex < batches.lastIndex) delay(REQUEST_SPACING_MILLIS)
   }
   Result.success()
  } catch (_: Exception) { Result.retry() }
 }
 private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 33 ||
  ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
 private fun createChannel(manager: NotificationManager) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(
   NotificationChannel(CHANNEL_ID, "Новые серии", NotificationManager.IMPORTANCE_DEFAULT).apply {
    description = "Новые эпизоды аниме из ваших подписок"
   }
  )
 }
 private fun showNotification(id: Int, title: String, episode: Int) {
  val intent = android.content.Intent(applicationContext, MainActivity::class.java)
  val pending = PendingIntent.getActivity(applicationContext, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
   .setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle(title)
   .setContentText("Вышла новая серия: " + episode)
   .setContentIntent(pending).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
  NotificationManagerCompat.from(applicationContext).notify(id, notification)
 }
 private fun stableId(animeId: String, episode: Int): Int = 31 * animeId.hashCode() + episode
 companion object {
  const val CHANNEL_ID = "yubera_new_episodes"
  const val MAX_IDS_PER_QUERY = 50
  const val REQUEST_SPACING_MILLIS = 700L
 }
}
