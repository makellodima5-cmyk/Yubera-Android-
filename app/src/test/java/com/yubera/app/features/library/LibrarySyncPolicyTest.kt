package com.yubera.app.features.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySyncPolicyTest {
    @Test
    fun newerTimestampWinsRegardlessOfDeviceId() {
        assertEquals(1, LibrarySyncPolicy.compareVersions(200, "a", 100, "z"))
        assertEquals(-1, LibrarySyncPolicy.compareVersions(100, "z", 200, "a"))
    }

    @Test
    fun sourceIdBreaksTimestampTiesDeterministically() {
        assertEquals(1, LibrarySyncPolicy.compareVersions(100, "web", 100, "android"))
        assertEquals(-1, LibrarySyncPolicy.compareVersions(100, "android", 100, "web"))
        assertEquals(0, LibrarySyncPolicy.compareVersions(100, "same", 100, "same"))
    }
}
