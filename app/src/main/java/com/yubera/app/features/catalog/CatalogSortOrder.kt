package com.yubera.app.features.catalog

import com.yubera.app.core.catalog.AnimeCatalogItem

enum class CatalogSortOrder(val title: String) {
    CATALOG("По каталогу"),
    RATING("По рейтингу"),
    YEAR("Сначала новые"),
    TITLE("По названию")
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
