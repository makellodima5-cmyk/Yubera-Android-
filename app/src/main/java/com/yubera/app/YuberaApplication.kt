package com.yubera.app

import android.app.Application
import com.yubera.app.features.library.LibrarySyncScheduler
import com.yubera.app.features.notifications.EpisodeNotificationScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YuberaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EpisodeNotificationScheduler.schedule(this)
        LibrarySyncScheduler.schedule(this)
    }
}
