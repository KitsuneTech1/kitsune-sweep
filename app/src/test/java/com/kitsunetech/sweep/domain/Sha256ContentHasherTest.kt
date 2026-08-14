package com.kitsunetech.sweep.domain

import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class Sha256ContentHasherTest {
    @Test
    fun hashesStreamWithoutLoadingWholeFile() = runBlocking {
        val hasher = Sha256ContentHasher { ByteArrayInputStream("hello".toByteArray()) }

        val hash = hasher.sha256(file("hello"))

        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            hash,
        )
    }

    private fun file(id: String) = StorageFile(
        id = id,
        displayName = "$id.bin",
        path = "/storage/$id.bin",
        contentUri = null,
        sizeBytes = 5L,
        modifiedAtMillis = 0L,
        mimeType = null,
        source = FileSource.DIRECT,
    )
}
