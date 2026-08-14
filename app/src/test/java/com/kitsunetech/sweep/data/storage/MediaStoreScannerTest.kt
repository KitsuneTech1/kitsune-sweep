package com.kitsunetech.sweep.data.storage

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreScannerTest {
    @Test
    fun filtersRowsAndSortsLargestFirst() = runBlocking {
        val rows = listOf(
            MediaStoreRecord("content://files/1", "small.zip", 50L, 10L, "application/zip"),
            MediaStoreRecord("content://files/2", "large.mp4", 300L, 20L, "video/mp4"),
            MediaStoreRecord("content://files/3", "exact.bin", 100L, 30L, null),
        )
        val progress = mutableListOf<ScanProgress>()

        val result = MediaStoreScanner { rows }.scanLargeFiles(100L) { progress += it }

        assertEquals(listOf("large.mp4", "exact.bin"), result.files.map { it.displayName })
        assertEquals(listOf(300L, 100L), result.files.map { it.sizeBytes })
        assertEquals(20_000L, result.files.first().modifiedAtMillis)
        assertTrue(progress.isNotEmpty())
    }
}
