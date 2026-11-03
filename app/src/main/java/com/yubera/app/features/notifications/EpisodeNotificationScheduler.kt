package com.yubera.app.features.notifications
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
object EpisodeNotificationScheduler {
 const val UNIQUE_WORK_NAME = "yubera-personal-episode-notifications"
 fun schedule(context: Context) {
  val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
  val request = PeriodicWorkRequestBuilder<EpisodeNotificationWorker>(6, TimeUnit.HOURS)
   .setConstraints(constraints).addTag(UNIQUE_WORK_NAME).build()
  WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
 }
}
