package com.yubera.app.features.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileFormattersTest {
    @Test
    fun formatsIsoTimestampAsLocalCalendarDate() {
        assertEquals("04.11.2026", formatRegistrationDate("2026-11-04T12:30:00Z"))
    }

    @Test
    fun formatsDateOnlyValue() {
        assertEquals("09.02.2025", formatRegistrationDate("2025-02-09"))
    }

    @Test
    fun returnsNullForMissingOrInvalidDate() {
        assertNull(formatRegistrationDate(null))
        assertNull(formatRegistrationDate(" "))
        assertNull(formatRegistrationDate("not-a-date"))
        assertNull(formatRegistrationDate("2026-02-30T00:00:00Z"))
    }
}
