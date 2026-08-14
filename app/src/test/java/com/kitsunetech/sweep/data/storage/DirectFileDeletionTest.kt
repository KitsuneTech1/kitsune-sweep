package com.kitsunetech.sweep.data.storage

import com.kitsunetech.sweep.domain.FileSafetyPolicy
import com.kitsunetech.sweep.domain.FileSource
import com.kitsunetech.sweep.domain.StorageFile
import com.kitsunetech.sweep.domain.buildDeletePlan
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNoException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DirectFileDeletionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun deletesOnlyFilesThatStillPassSafetyChecks() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val allowed = Files.write(root.resolve("allowed.bin"), "allowed".toByteArray())
        val outside = temporaryFolder.newFile("outside.bin").toPath()
        val protected = Files.createDirectories(root.resolve("Android")).resolve("blocked.bin")
        Files.write(protected, "blocked".toByteArray())
        val directory = Files.createDirectory(root.resolve("folder"))
        val plan = buildDeletePlan(
            listOf(
                item("allowed", allowed),
                item("outside", outside),
                item("protected", protected),
                item("directory", directory),
            ),
        )

        val result = deleteConfirmedDirectFiles(
            plan = plan,
            roots = setOf(root),
            safetyPolicy = FileSafetyPolicy(),
        )

        if (supportsAnchoredDirectDeletion(root)) {
            assertEquals(listOf("allowed"), result.deletedIds)
            assertEquals(setOf("outside", "protected", "directory"), result.failedIds.toSet())
            assertFalse(Files.exists(allowed))
        } else {
            assertEquals(emptyList<String>(), result.deletedIds)
            assertEquals(setOf("allowed", "outside", "protected", "directory"), result.failedIds.toSet())
            assertTrue(Files.exists(allowed))
        }
        assertTrue(Files.exists(outside))
        assertTrue(Files.exists(protected))
        assertTrue(Files.isDirectory(directory))
    }

    @Test
    fun failsClosedForAnAncestorLinkButContinuesWithOtherFiles() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val allowed = Files.write(root.resolve("allowed.bin"), "allowed".toByteArray())
        val outside = temporaryFolder.newFolder("outside").toPath()
        val outsideTarget = Files.write(outside.resolve("outside.bin"), "outside".toByteArray())
        val linkedDirectory = root.resolve("linked")
        try {
            Files.createSymbolicLink(linkedDirectory, outside)
        } catch (error: Exception) {
            assumeNoException(error)
        }

        val result = deleteConfirmedDirectFiles(
            plan = buildDeletePlan(listOf(item("allowed", allowed), item("linked", linkedDirectory.resolve("outside.bin")))),
            roots = setOf(root),
            safetyPolicy = FileSafetyPolicy(),
        )

        if (supportsAnchoredDirectDeletion(root)) {
            assertEquals(listOf("allowed"), result.deletedIds)
            assertEquals(listOf("linked"), result.failedIds)
            assertFalse(Files.exists(allowed))
        } else {
            assertEquals(emptyList<String>(), result.deletedIds)
            assertEquals(setOf("allowed", "linked"), result.failedIds.toSet())
            assertTrue(Files.exists(allowed))
        }
        assertTrue(Files.exists(outsideTarget))
    }

    @Test
    fun failsClosedWhenAPreplannedParentIsSwappedForALink() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val originalParent = Files.createDirectory(root.resolve("Downloads"))
        val originalTarget = Files.write(originalParent.resolve("target.bin"), "original".toByteArray())
        val outside = temporaryFolder.newFolder("outside").toPath()
        val outsideTarget = Files.write(outside.resolve("target.bin"), "outside".toByteArray())
        val planned = item("swapped", originalTarget)

        Files.move(originalParent, root.resolve("Downloads-original"))
        try {
            Files.createSymbolicLink(root.resolve("Downloads"), outside)
        } catch (error: Exception) {
            assumeNoException(error)
        }

        val result = deleteConfirmedDirectFiles(
            plan = buildDeletePlan(listOf(planned)),
            roots = setOf(root),
            safetyPolicy = FileSafetyPolicy(),
        )

        assertEquals(emptyList<String>(), result.deletedIds)
        assertEquals(listOf("swapped"), result.failedIds)
        assertTrue(Files.exists(root.resolve("Downloads-original/target.bin")))
        assertTrue(Files.exists(outsideTarget))
    }

    private fun item(id: String, path: java.nio.file.Path) = StorageFile(
        id = id,
        displayName = path.fileName.toString(),
        path = path.toString(),
        contentUri = null,
        sizeBytes = runCatching { Files.size(path) }.getOrDefault(0L),
        modifiedAtMillis = 0L,
        mimeType = null,
        source = FileSource.DIRECT,
    )
}
