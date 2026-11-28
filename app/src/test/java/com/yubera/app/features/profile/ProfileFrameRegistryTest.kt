package com.yubera.app.features.profile

import com.yubera.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileFrameRegistryTest {
    @Test
    fun restoredNineteenthFrameIsSelectableAndResourceMatches() {
        assertEquals(9, adminFrames.size)
        assertEquals(19, adminFrames.size + 5 + 5)
        assertEquals("Ramka_Admin_01.webp", adminFrames.last().id)
        assertEquals(R.drawable.ramka_admin_01, adminFrames.last().resource)
    }

    @Test
    fun selectedFrameCategoryIsRestored() {
        assertEquals("admin", profileFrameCategory("Admin_Frame_03.webp", true))
        assertEquals("admin", profileFrameCategory("Ramka_Admin_01.webp", true))
        assertEquals("support", profileFrameCategory("Ramka_Admin_01.webp", false))
        assertEquals("plus", profileFrameCategory("YUBERA_Plus_Avatar_Frame_02_Mystic_Kitsune.webp", false))
        assertEquals("support", profileFrameCategory(null, false))
    }
}
