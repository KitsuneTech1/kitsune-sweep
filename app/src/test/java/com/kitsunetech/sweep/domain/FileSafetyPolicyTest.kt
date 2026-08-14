package com.kitsunetech.sweep.domain

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNoException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileSafetyPolicyTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val policy = FileSafetyPolicy()

    @Test
    fun allowsRegularFileInsideSharedRoot() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val file = Files.write(root.resolve("video.mp4"), "safe".toByteArray())

        assertTrue(policy.canDelete(file, setOf(root)))
    }

    @Test
    fun rejectsStorageRootAndDirectories() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val directory = Files.createDirectory(root.resolve("Downloads"))

        assertFalse(policy.canDelete(root, setOf(root)))
        assertFalse(policy.canDelete(directory, setOf(root)))
    }

    @Test
    fun rejectsAndroidTree() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val androidData = Files.createDirectories(root.resolve("Android/data"))
        val file = Files.write(androidData.resolve("cache.bin"), "blocked".toByteArray())

        assertFalse(policy.canDelete(file, setOf(root)))
    }

    @Test
    fun rejectsFileOutsideAllowedRoot() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val outside = temporaryFolder.newFile("outside.bin").toPath()

        assertFalse(policy.canDelete(outside, setOf(root)))
    }

    @Test
    fun rejectsSymbolicLinks() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val target = Files.write(root.resolve("target.bin"), "target".toByteArray())
        val link = root.resolve("link.bin")

        try {
            Files.createSymbolicLink(link, target)
        } catch (error: Exception) {
            assumeNoException(error)
        }

        assertFalse(policy.canDelete(link, setOf(root)))
    }

    @Test
    fun rejectsAnExistingAncestorSymbolicLink() {
        val root = temporaryFolder.newFolder("shared").toPath()
        val outside = temporaryFolder.newFolder("outside").toPath()
        val target = Files.write(outside.resolve("target.bin"), "outside".toByteArray())
        val linkedDirectory = root.resolve("linked")

        try {
            Files.createSymbolicLink(linkedDirectory, outside)
        } catch (error: Exception) {
            assumeNoException(error)
        }

        assertFalse(policy.canDelete(linkedDirectory.resolve(target.fileName), setOf(root)))
    }

    @Test
    fun rejectsConfiguredPrivateRoots() {
        val shared = temporaryFolder.newFolder("shared").toPath()
        val privateRoot = temporaryFolder.newFolder("private").toPath()
        val privateFile = Files.write(privateRoot.resolve("secret.bin"), "secret".toByteArray())
        val guardedPolicy = FileSafetyPolicy(protectedRoots = setOf(privateRoot))

        assertFalse(guardedPolicy.canDelete(privateFile, setOf(shared, privateRoot)))
    }
}
