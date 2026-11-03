package com.yubera.app.features.library

import android.content.Context
import androidx.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerLibrarySyncTrigger @Inject constructor(
    @ApplicationContext private val context: Context
) : LibrarySyncTrigger {
    override fun enqueue() {
        LibrarySyncScheduler.enqueueNow(context)
    }
}
