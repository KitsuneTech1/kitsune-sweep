package com.kitsunetech.sweep.data.storage

import java.io.RandomAccessFile
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DirectStorageScannerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun keepsThresholdMatchesAndSortsLargestFirst() = runBlocking {
        val root = temporaryFolder.newFolder("shared").toPath()
        createSparseFile(root.resolve("small.bin"), 20L * MEBIBYTE)
        createSparseFile(root.resolve("exact.bin"), 100L * MEBIBYTE)
        createSparseFile(root.resolve("large.bin"), 150L * MEBIBYTE)
        Files.createDirectories(root.resolve("nested"))

        val progress = mutableListOf<ScanProgress>()
        val result = DirectStorageScanner(setOf(root)).scanLargeFiles(100L * MEBIBYTE) {
            progress += it
        }

        assertEquals(listOf("large.bin", "exact.bin"), result.files.map { it.displayName })
        assertEquals(listOf(150L * MEBIBYTE, 100L * MEBIBYTE), result.files.map { it.sizeBytes })
        assertTrue(progress.isNotEmpty())
        assertTrue(result.errors.isEmpty())
    }

    private fun createSparseFile(path: java.nio.file.Path, size: Long) {
        RandomAccessFile(path.toFile(), "rw").use { it.setLength(size) }
    }

    private companion object {
        const val MEBIBYTE = 1024L * 1024L
    }
}
