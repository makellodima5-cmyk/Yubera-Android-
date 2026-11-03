package com.yubera.app.features.library

object LibrarySyncPolicy {
    /** Positive means the left-hand version wins, negative means the right-hand version wins. */
    fun compareVersions(
        leftTimestamp: Long,
        leftSourceId: String,
        rightTimestamp: Long,
        rightSourceId: String
    ): Int {
        val timestampOrder = leftTimestamp.compareTo(rightTimestamp)
        return if (timestampOrder != 0) timestampOrder else leftSourceId.compareTo(rightSourceId)
    }
}
