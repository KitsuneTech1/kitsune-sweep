package com.kitsunetech.sweep.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectorTest {
    @Test
    fun hashesOnlySameSizeCandidatesAndGroupsEqualContent() = runBlocking {
        val unique = file("unique", 40L)
        val first = file("first", 100L)
        val second = file("second", 100L)
        val different = file("different", 100L)
        val hasher = RecordingHasher(
            mapOf(
                "first" to "same-hash",
                "second" to "same-hash",
                "different" to "other-hash",
            ),
        )

        val groups = DuplicateDetector(hasher).findExact(
            files = listOf(unique, first, second, different),
            onProgress = {},
        )

        assertFalse("unique-size file must not be read", hasher.hashedIds.contains("unique"))
        assertEquals(setOf("first", "second", "different"), hasher.hashedIds.toSet())
        assertEquals(1, groups.size)
        assertEquals(listOf("first", "second"), groups.single().files.map { it.id })
        assertEquals(100L, groups.single().reclaimableBytes)
    }

    @Test
    fun ignoresEmptyFilesAndReportsHashProgress() = runBlocking {
        val hasher = RecordingHasher(mapOf("a" to "hash", "b" to "hash"))
        val progress = mutableListOf<HashProgress>()

        val groups = DuplicateDetector(hasher).findExact(
            files = listOf(file("empty-a", 0L), file("empty-b", 0L), file("a", 5L), file("b", 5L)),
            onProgress = { progress += it },
        )

        assertEquals(1, groups.size)
        assertTrue(progress.isNotEmpty())
        assertEquals(2, progress.last().completed)
        assertEquals(2, progress.last().total)
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

    private class RecordingHasher(
        private val hashes: Map<String, String>,
    ) : ContentHasher {
        val hashedIds = mutableListOf<String>()

        override suspend fun sha256(file: StorageFile): String {
            hashedIds += file.id
            return hashes.getValue(file.id)
        }
    }
}
