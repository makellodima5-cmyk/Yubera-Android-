package com.yubera.app.features.catalog

import com.yubera.app.core.catalog.AnimeCatalogItem

enum class CatalogSortOrder(val title: String) {
    CATALOG("По каталогу"),
    RATING("По рейтингу"),
    YEAR("Сначала новые"),
    TITLE("По названию")
}

fun List<AnimeCatalogItem>.filterForCatalog(
    query: String,
    genre: String?,
    year: Int?,
    status: String?
): List<AnimeCatalogItem> {
    val normalizedQuery = query.trim().lowercase()
    return filter { anime ->
        val matchesQuery = normalizedQuery.isBlank() ||
            anime.title.lowercase().contains(normalizedQuery) ||
            (anime.englishTitle?.lowercase()?.contains(normalizedQuery) == true) ||
            anime.originalTitle.lowercase().contains(normalizedQuery)
        val matchesGenre = genre == null || genre in anime.genres
        val matchesYear = year == null || anime.year == year
        val matchesStatus = status == null || anime.status == status
        matchesQuery && matchesGenre && matchesYear && matchesStatus
    }
}

fun List<AnimeCatalogItem>.sortedForCatalog(order: CatalogSortOrder): List<AnimeCatalogItem> =
    when (order) {
        CatalogSortOrder.CATALOG -> this
        CatalogSortOrder.RATING -> sortedWith(
            compareByDescending<AnimeCatalogItem> { it.rating ?: Double.NEGATIVE_INFINITY }
                .thenBy { it.title.lowercase() }
        )
        CatalogSortOrder.YEAR -> sortedWith(
            compareByDescending<AnimeCatalogItem> { it.year.takeIf { year -> year > 0 } ?: Int.MIN_VALUE }
                .thenBy { it.title.lowercase() }
        )
        CatalogSortOrder.TITLE -> sortedBy { it.title.lowercase() }
    }
