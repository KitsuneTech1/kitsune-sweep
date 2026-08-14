package com.kitsunetech.sweep.data.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kitsunetech.sweep.domain.FileSource
import com.kitsunetech.sweep.domain.Sha256ContentHasher
import com.kitsunetech.sweep.domain.StorageFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidContentStreamOpenerInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun hashesDirectFileThroughAndroidOpener() = runBlocking {
        val localFile = context.cacheDir.resolve("kitsune-sweep-hash-test.bin")
        localFile.writeBytes("hello".toByteArray())
        val file = StorageFile(
            id = localFile.absolutePath,
            displayName = localFile.name,
            path = localFile.absolutePath,
            contentUri = null,
            sizeBytes = localFile.length(),
            modifiedAtMillis = localFile.lastModified(),
            mimeType = null,
            source = FileSource.DIRECT,
        )

        try {
            val hash = Sha256ContentHasher(AndroidContentStreamOpener(context)).sha256(file)

            assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                hash,
            )
        } finally {
            localFile.delete()
        }
    }
}
