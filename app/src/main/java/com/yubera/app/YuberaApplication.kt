package com.yubera.app

import android.app.Application
import com.yubera.app.features.catalog.CatalogUpdateScheduler
import com.yubera.app.features.library.LibrarySyncScheduler
import com.yubera.app.features.notifications.EpisodeNotificationScheduler
import com.yubera.app.features.updates.UpdateCheckScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YuberaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CatalogUpdateScheduler.schedule(this)
        UpdateCheckScheduler.schedule(this)
        EpisodeNotificationScheduler.schedule(this)
        LibrarySyncScheduler.schedule(this)
    }
}
