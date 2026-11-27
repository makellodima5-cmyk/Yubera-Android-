package com.yubera.app.features.catalog

import com.yubera.app.core.catalog.AnimeCatalogItem

enum class CatalogSortOrder(val title: String) {
    CATALOG("По каталогу"), RATING("По рейтингу"), YEAR("Сначала новые"), TITLE("По названию")
}

data class CatalogFilters(
    val status: String? = null,
    val kind: String? = null,
    val genres: Set<String> = emptySet(),
    val ageRating: String? = null,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val minRating: Double = 0.0
)

fun List<AnimeCatalogItem>.filterForCatalog(query: String, genre: String?, year: Int?, status: String?): List<AnimeCatalogItem> =
    filterForCatalog(query, CatalogFilters(status=status, genres=genre?.let(::setOf) ?: emptySet(), yearFrom=year, yearTo=year))

fun List<AnimeCatalogItem>.filterForCatalog(query: String, filters: CatalogFilters): List<AnimeCatalogItem> {
    val q=query.trim().lowercase()
    return filter { a ->
        (q.isBlank() || a.title.lowercase().contains(q) || (a.englishTitle?.lowercase()?.contains(q)==true) || a.originalTitle.lowercase().contains(q)) &&
        (filters.status==null || a.status.equals(filters.status,true)) &&
        (filters.kind==null || a.kind.equals(filters.kind,true)) &&
        (filters.genres.isEmpty() || filters.genres.all { it in a.genres }) &&
        (filters.ageRating==null || a.ageRating.equals(filters.ageRating,true)) &&
        (filters.yearFrom==null || a.year>=filters.yearFrom) &&
        (filters.yearTo==null || a.year<=filters.yearTo) &&
        (filters.minRating<=0.0 || (a.rating ?: 0.0)>=filters.minRating)
    }
}

fun List<AnimeCatalogItem>.sortedForCatalog(order: CatalogSortOrder): List<AnimeCatalogItem> = when(order) {
    CatalogSortOrder.CATALOG -> this
    CatalogSortOrder.RATING -> sortedWith(compareByDescending<AnimeCatalogItem>{it.rating ?: Double.NEGATIVE_INFINITY}.thenBy{it.title.lowercase()})
    CatalogSortOrder.YEAR -> sortedWith(compareByDescending<AnimeCatalogItem>{it.year.takeIf{y->y>0} ?: Int.MIN_VALUE}.thenBy{it.title.lowercase()})
    CatalogSortOrder.TITLE -> sortedBy{it.title.lowercase()}
}

fun CatalogFilters.validate(maxYear: Int): String? {
    if (yearFrom != null && yearFrom !in 1900..maxYear) return "Укажите корректный год «От»."
    if (yearTo != null && yearTo !in 1900..maxYear) return "Укажите корректный год «До»."
    if (yearFrom != null && yearTo != null && yearFrom > yearTo) return "Год «От» не может быть больше года «До»."
    return null
}
