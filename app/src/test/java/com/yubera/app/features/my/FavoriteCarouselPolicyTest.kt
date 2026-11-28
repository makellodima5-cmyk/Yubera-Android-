package com.yubera.app.features.my

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteCarouselPolicyTest {
    @Test fun emptyAndSingleFavoriteDoNotCreateDuplicateCards() {
        assertEquals(emptyList<Int>(), favoriteCarouselOffsets(0))
        assertEquals(listOf(0), favoriteCarouselOffsets(1))
    }

    @Test fun twoFavoritesShowExactlyTwoUniquePositions() {
        assertEquals(listOf(0, 1), favoriteCarouselOffsets(2))
    }

    @Test fun threeAndFourFavoritesShowThreeNearestCards() {
        assertEquals(listOf(-1, 0, 1), favoriteCarouselOffsets(3))
        assertEquals(listOf(-1, 0, 1), favoriteCarouselOffsets(4))
    }

    @Test fun largerListsShowFiveCards() {
        assertEquals(listOf(-2, -1, 0, 1, 2), favoriteCarouselOffsets(5))
        assertEquals(listOf(-2, -1, 0, 1, 2), favoriteCarouselOffsets(20))
    }
}
