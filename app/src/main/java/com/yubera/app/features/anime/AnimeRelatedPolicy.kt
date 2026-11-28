package com.yubera.app.features.anime

import com.yubera.app.core.catalog.AnimeCatalogItem

internal object AnimeRelatedPolicy {
    fun select(current: AnimeCatalogItem?, catalog: List<AnimeCatalogItem>, relatedIds: List<String>): List<AnimeCatalogItem> {
        if (current == null) return emptyList()
        val byId = catalog.associateBy(AnimeCatalogItem::id)
        val linked = relatedIds.mapNotNull(byId::get).filter { it.id != current.id }
        val genreMatches = catalog
            .filter { candidate -> candidate.id != current.id && candidate.genres.any(current.genres::contains) }
            .sortedByDescending { candidate -> candidate.genres.count(current.genres::contains) }
        return (linked + genreMatches).distinctBy(AnimeCatalogItem::id).take(12)
    }
}
