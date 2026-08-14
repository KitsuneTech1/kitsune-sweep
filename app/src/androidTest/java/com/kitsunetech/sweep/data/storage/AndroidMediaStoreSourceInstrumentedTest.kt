package com.kitsunetech.sweep.data.storage

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMediaStoreSourceInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver

    @Test
    fun returnsAFileInsertedByTheApp() = runBlocking {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "kitsune-sweep-source-test.bin")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/KitsuneSweepTests")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        assertNotNull(uri)

        try {
            resolver.openOutputStream(uri!!)!!.use { it.write(ByteArray(128) { 7 }) }

            val rows = AndroidMediaStoreSource(context).load()

            assertTrue(
                rows.any {
                    it.displayName == "kitsune-sweep-source-test.bin" && it.sizeBytes == 128L
                },
            )
        } finally {
            uri?.let { resolver.delete(it, null, null) }
        }
    }
}
