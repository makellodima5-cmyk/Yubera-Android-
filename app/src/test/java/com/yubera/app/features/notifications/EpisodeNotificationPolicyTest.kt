package com.yubera.app.features.notifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class EpisodeNotificationPolicyTest {
 @Test fun firstCheckSetsBaselineWithoutSendingOldEpisodes() {
  assertEquals(emptyList<Int>(), EpisodeNotificationPolicy.releasedEpisodes(null, 12))
  assertEquals(12, EpisodeNotificationPolicy.nextWatermark(null, 12))
 }
 @Test fun newlyReleasedEpisodesAreReturnedExactlyOnceAsRange() {
  assertEquals(listOf(9, 10, 11), EpisodeNotificationPolicy.releasedEpisodes(8, 11))
 }
 @Test fun staleApiResponseDoesNotMoveWatermarkBackwards() {
  assertNull(EpisodeNotificationPolicy.nextWatermark(12, 10))
  assertEquals(emptyList<Int>(), EpisodeNotificationPolicy.releasedEpisodes(12, 10))
 }
 @Test fun sameEpisodeCountDoesNotCreateNotification() {
  assertEquals(emptyList<Int>(), EpisodeNotificationPolicy.releasedEpisodes(10, 10))
  assertEquals(10, EpisodeNotificationPolicy.nextWatermark(10, 10))
 }
}
