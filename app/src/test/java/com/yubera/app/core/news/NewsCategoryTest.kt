package com.yubera.app.core.news

import org.junit.Assert.assertEquals
import org.junit.Test

class NewsCategoryTest {
    @Test
    fun classifiesInterviewsAndNewSeasons() {
        assertEquals(NewsCategory.INTERVIEWS, NewsCategory.classify("Интервью с режиссёром", ""))
        assertEquals(NewsCategory.NEW_SEASONS, NewsCategory.classify("Анонсирован второй сезон", ""))
    }

    @Test
    fun classifiesEventsAndGeneralIndustryNews() {
        assertEquals(NewsCategory.EVENTS, NewsCategory.classify("Победители фестиваля", ""))
        assertEquals(NewsCategory.INDUSTRY, NewsCategory.classify("Изменения в производстве аниме", ""))
    }
}
