package com.yubera.app.navigation
import org.junit.Assert.assertEquals
import org.junit.Test
class AppDestinationTest {
    @Test
    fun allFiveDestinationsHaveUniqueRoutesAndRussianLabels() {
        assertEquals(5, AppDestination.all.size)
        assertEquals(5, AppDestination.all.map { it.route }.toSet().size)
        assertEquals(listOf("Главная", "Каталог", "Моё", "Новости", "Профиль"), AppDestination.all.map { it.label })
    }
}
