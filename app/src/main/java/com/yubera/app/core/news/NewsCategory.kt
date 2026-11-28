package com.yubera.app.core.news

import java.util.Locale

enum class NewsCategory(val title: String) {
    ALL("Все"),
    ANNOUNCEMENTS("Анонсы"),
    NEW_SEASONS("Новые сезоны"),
    INDUSTRY("Индустрия"),
    EVENTS("События"),
    INTERVIEWS("Интервью");

    companion object {
        private val phrases: Map<NewsCategory, List<String>> = mapOf(
            INTERVIEWS to listOf(
                "интервью", "беседа с", "поговорили с", "рассказал в интервью",
                "interview with", "interview", "q&a with"
            ),
            NEW_SEASONS to listOf(
                "второй сезон", "третий сезон", "четвертый сезон", "четвёртый сезон",
                "новый сезон", "получит продолжение", "продолжение сериала",
                "анонсирован второй сезон", "анонсирован третий сезон",
                "second season", "third season", "new season", "season 2", "season 3", "sequel"
            ),
            ANNOUNCEMENTS to listOf(
                "анонсирован", "анонсирована", "анонсировано", "анонсировали",
                "получил анонс", "получила анонс", "получит аниме-адаптацию",
                "аниме-адаптация", "получит адаптацию", "дебютный трейлер",
                "вышел тизер", "показали тизер", "дата премьеры", "премьера назначена",
                "announced", "anime adaptation", "premiere date", "teaser released",
                "trailer released"
            ),
            EVENTS to listOf(
                "аниме-фестиваль", "фестиваль аниме", "фестиваль", "конвенция",
                "выставка", "конкурс", "церемония награждения", "премия",
                "anime festival", "convention", "expo", "award ceremony"
            ),
            INDUSTRY to listOf(
                "анимационная студия", "производство аниме", "производстве аниме",
                "индустрия аниме", "производственный комитет", "кадровые изменения",
                "кадровые перестановки", "смена режиссера", "смена режиссёра",
                "студия закрывает", "приобрела права", "приобретение студии",
                "anime production", "anime industry", "production committee",
                "studio acquisition", "staff changes"
            )
        )

        fun classify(title: String, body: String): NewsCategory {
            val normalizedTitle = normalize(title)
            val normalizedBody = normalize(body)
            val scored = phrases.map { (category, candidates) ->
                val score = candidates.sumOf { phrase ->
                    when {
                        normalizedTitle.contains(phrase) -> 3
                        normalizedBody.contains(phrase) -> 1
                        else -> 0
                    }
                }
                category to score
            }
            val bestScore = scored.maxOfOrNull { it.second } ?: 0
            if (bestScore < MIN_CONFIDENT_SCORE) return ALL
            val winners = scored.filter { it.second == bestScore }
            return winners.singleOrNull()?.first ?: ALL
        }

        private fun normalize(value: String): String =
            value.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

        private const val MIN_CONFIDENT_SCORE = 3
    }
}
