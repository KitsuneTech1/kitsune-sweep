package com.kitsunetech.sweep.data.storage

import android.util.Log
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import java.nio.file.Files
import java.nio.file.SecureDirectoryStream
import org.junit.Test
import org.junit.Assert.assertTrue

class SecureDirectoryStreamInstrumentedTest {
    @Test
    fun glassProviderSupportsAnchoredDeletion() {
        InstrumentationRegistry.getInstrumentation().targetContext
        val sharedRoot = Environment.getExternalStorageDirectory().toPath()
        val result = runCatching {
            Files.newDirectoryStream(sharedRoot).use { stream ->
                "root=$sharedRoot provider=${stream.javaClass.name} supported=${stream is SecureDirectoryStream<*>}"
            }
        }
        result.onSuccess { details ->
            Log.i(
                "KitsuneSweep",
                "SecureDirectoryStream $details",
            )
        }
        result.onFailure { error ->
            Log.i("KitsuneSweep", "SecureDirectoryStream unavailable=${error.javaClass.simpleName}: ${error.message}")
        }
        assertTrue(
            "Direct deletion requires SecureDirectoryStream at $sharedRoot, but found ${result.getOrNull() ?: result.exceptionOrNull()}",
            result.getOrNull()?.contains("supported=true") == true,
        )
    }
}
