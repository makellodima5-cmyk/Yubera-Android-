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
    val queryVariants = searchVariants(query)
    return filter { a ->
        (query.isBlank() || listOf(a.title, a.englishTitle.orEmpty(), a.originalTitle).any { title ->
            val variants = searchVariants(title)
            queryVariants.any { q -> variants.any { it.contains(q) || fuzzyPrefixMatch(it, q) } }
        }) &&
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

fun searchPriority(anime:AnimeCatalogItem, query:String):Int {
 if(query.isBlank()) return 0
 val qs=searchVariants(query)
 val ts=listOf(anime.title,anime.englishTitle.orEmpty(),anime.originalTitle).flatMap(::searchVariants)
 return when {
  qs.any{q->ts.any{it==q}}->0
  qs.any{q->ts.any{it.startsWith(q)}}->1
  qs.any{q->ts.any{it.contains(q)}}->2
  else->3
}
}

private fun searchVariants(value: String): Set<String> {
    val raw=value.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}]"),"")
    if(raw.isEmpty()) return setOf("")
    val latinToCyrillic=mapOf('q' to 'й','w' to 'ц','e' to 'у','r' to 'к','t' to 'е','y' to 'н','u' to 'г','i' to 'ш','o' to 'щ','p' to 'з','[' to 'х',']' to 'ъ','a' to 'ф','s' to 'ы','d' to 'в','f' to 'а','g' to 'п','h' to 'р','j' to 'о','k' to 'л','l' to 'д',';' to 'ж','z' to 'я','x' to 'ч','c' to 'с','v' to 'м','b' to 'и','n' to 'т','m' to 'ь')
    val cyrillicToLatin=mapOf('й' to 'q','ц' to 'w','у' to 'e','к' to 'r','е' to 't','н' to 'y','г' to 'u','ш' to 'i','щ' to 'o','з' to 'p','х' to '[','ъ' to ']','ф' to 'a','ы' to 's','в' to 'd','а' to 'f','п' to 'g','р' to 'h','о' to 'j','л' to 'k','д' to 'l','ж' to ';','я' to 'z','ч' to 'x','с' to 'c','м' to 'v','и' to 'b','т' to 'n','ь' to 'm')
    val visuallySimilar=mapOf('а' to 'a','в' to 'b','е' to 'e','к' to 'k','м' to 'm','н' to 'h','о' to 'o','р' to 'p','с' to 'c','т' to 't','у' to 'y','х' to 'x')
    val keyboardCyr=raw.map{latinToCyrillic[it] ?: it}.joinToString("")
    val keyboardLat=raw.map{cyrillicToLatin[it] ?: it}.joinToString("")
    val visualLat=raw.map{visuallySimilar[it] ?: it}.joinToString("")
    val visualCyr=visualLat.map{latinToCyrillic[it] ?: it}.joinToString("")
    val latinVisualToCyrillic=mapOf('a' to 'а','b' to 'в','e' to 'е','k' to 'к','m' to 'м','h' to 'н','o' to 'о','p' to 'р','c' to 'с','t' to 'т','y' to 'у','x' to 'х')
    val reverseVisualCyr=raw.map{latinVisualToCyrillic[it] ?: it}.joinToString("")
    return setOf(raw,keyboardCyr,keyboardLat,visualLat,visualCyr,reverseVisualCyr)
}
private fun fuzzyPrefixMatch(title:String, query:String):Boolean {
    if(query.length<3 || query.length>title.length) return false
    val prefix=title.take(query.length+2)
    var qi=0
    for(ch in prefix) if(qi<query.length && ch==query[qi]) qi++
    return qi>=query.length-1
}
