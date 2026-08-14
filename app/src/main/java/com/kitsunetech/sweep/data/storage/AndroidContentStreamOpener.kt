package com.kitsunetech.sweep.data.storage

import android.content.Context
import android.net.Uri
import com.kitsunetech.sweep.domain.ContentStreamOpener
import com.kitsunetech.sweep.domain.StorageFile
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class AndroidContentStreamOpener(
    context: Context,
) : ContentStreamOpener {
    private val resolver = context.applicationContext.contentResolver

    override fun open(file: StorageFile): InputStream {
        file.contentUri?.let { uri ->
            return resolver.openInputStream(Uri.parse(uri))
                ?: throw IOException("Android could not open ${file.displayName}")
        }
        file.path?.let { path -> return FileInputStream(File(path)) }
        throw IOException("No readable location for ${file.displayName}")
    }
}
