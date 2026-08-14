package com.kitsunetech.sweep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletePlanTest {
    @Test
    fun emptySelectionHasNoBytes() {
        val plan = buildDeletePlan(emptyList())

        assertTrue(plan.files.isEmpty())
        assertEquals(0L, plan.totalBytes)
    }

    @Test
    fun deduplicatesIdsClampsNegativeSizesAndSortsLargestFirst() {
        val large = file("large", 300L)
        val duplicateLarge = file("large", 300L)
        val small = file("small", 20L)
        val invalid = file("invalid", -50L)

        val plan = buildDeletePlan(listOf(small, large, duplicateLarge, invalid))

        assertEquals(listOf("large", "small", "invalid"), plan.files.map { it.id })
        assertEquals(320L, plan.totalBytes)
    }

    private fun file(id: String, size: Long) = StorageFile(
        id = id,
        displayName = "$id.bin",
        path = "/storage/$id.bin",
        contentUri = null,
        sizeBytes = size,
        modifiedAtMillis = 0L,
        mimeType = null,
        source = FileSource.DIRECT,
    )
}
