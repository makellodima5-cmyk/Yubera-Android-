package com.yubera.app.features.library

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object LibrarySyncScheduler {
    const val UNIQUE_PERIODIC_WORK = "yubera-personal-library-sync"
    const val UNIQUE_IMMEDIATE_WORK = "yubera-personal-library-sync-immediate"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<LibrarySyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(UNIQUE_PERIODIC_WORK)
            .build()
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, periodic)
        enqueueNow(context)
    }

    fun enqueueNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
            .setConstraints(constraints)
            .addTag(UNIQUE_IMMEDIATE_WORK)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
