package com.yubera.app.features.notifications

object EpisodeNotificationPolicy {
    fun shouldDeliverNotification(
        notificationsEnabled: Boolean,
        newEpisodeNotificationsEnabled: Boolean,
        subscriptionNotificationsEnabled: Boolean,
        systemNotificationsEnabled: Boolean
    ): Boolean = notificationsEnabled &&
        newEpisodeNotificationsEnabled &&
        subscriptionNotificationsEnabled &&
        systemNotificationsEnabled

    fun releasedEpisodes(previousCount: Int?, currentCount: Int): List<Int> {
        if (previousCount == null || currentCount <= previousCount) return emptyList()
        return (previousCount + 1..currentCount).toList()
    }

    fun nextWatermark(previousCount: Int?, currentCount: Int): Int? =
        if (previousCount == null || currentCount >= previousCount) currentCount else null
}
