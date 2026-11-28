package com.yubera.app.features.catalog

const val CATALOG_PAGE_SIZE = 50

fun catalogPageCount(itemCount: Int, pageSize: Int = CATALOG_PAGE_SIZE): Int {
    if (itemCount <= 0 || pageSize <= 0) return 0
    return (itemCount + pageSize - 1) / pageSize
}

fun <T> catalogPageItems(items: List<T>, page: Int, pageSize: Int = CATALOG_PAGE_SIZE): List<T> {
    if (pageSize <= 0) return emptyList()
    val safePage = page.coerceAtLeast(0)
    val start = safePage * pageSize
    if (start >= items.size) return emptyList()
    return items.drop(start).take(pageSize)
}
