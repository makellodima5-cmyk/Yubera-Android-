package com.yubera.app.features.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogPaginationPolicyTest {
    @Test fun catalogHas50ItemsPerPage() {
        val items = (1..120).toList()
        assertEquals(50, catalogPageItems(items, 0).size)
        assertEquals((1..50).toList(), catalogPageItems(items, 0))
        assertEquals((51..100).toList(), catalogPageItems(items, 1))
        assertEquals((101..120).toList(), catalogPageItems(items, 2))
    }

    @Test fun pageCountRoundsUpAndEmptyCatalogHasNoPages() {
        assertEquals(0, catalogPageCount(0))
        assertEquals(1, catalogPageCount(1))
        assertEquals(1, catalogPageCount(50))
        assertEquals(2, catalogPageCount(51))
        assertEquals(3, catalogPageCount(120))
    }

    @Test fun invalidAndOutOfRangePagesReturnEmpty() {
        val items = (1..5).toList()
        assertEquals(items, catalogPageItems(items, -1))
        assertEquals(emptyList<Int>(), catalogPageItems(items, 1))
        assertEquals(emptyList<Int>(), catalogPageItems(items, 0, 0))
    }
}
