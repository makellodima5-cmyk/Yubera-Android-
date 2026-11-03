package com.yubera.app.core.news

enum class NewsCategory(val title: String) {
    ALL("Все"),
    ANNOUNCEMENTS("Анонсы"),
    NEW_SEASONS("Новые сезоны"),
    INDUSTRY("Индустрия"),
    EVENTS("События"),
    INTERVIEWS("Интервью");

    companion object {
        fun classify(title: String, summary: String): NewsCategory {
            val text = "$title $summary".lowercase()
            return when {
                listOf("интервью", "interview").any(text::contains) -> INTERVIEWS
                listOf("второй сезон", "новый сезон", "продолжение", "season 2", "new season", "sequel", "сезон 2").any(text::contains) -> NEW_SEASONS
                listOf("анонс", "тизер", "трейлер", "announcement", "announced", "trailer", "teaser").any(text::contains) -> ANNOUNCEMENTS
                listOf("фестивал", "победител", "выстав", "преми", "наград", "конвент", "festival", "award", "convention", "event").any(text::contains) -> EVENTS
                else -> INDUSTRY
            }
        }
    }
}
