package com.kitsunetech.sweep.data.storage

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidMediaStoreSource(
    context: Context,
) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun load(): List<MediaStoreRecord> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )
        val rows = mutableListOf<MediaStoreRecord>()

        resolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Files.FileColumns.SIZE} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                rows += MediaStoreRecord(
                    contentUri = ContentUris.withAppendedId(collection, id).toString(),
                    displayName = cursor.getString(nameColumn),
                    sizeBytes = cursor.getLong(sizeColumn).coerceAtLeast(0L),
                    modifiedAtSeconds = cursor.getLong(modifiedColumn).coerceAtLeast(0L),
                    mimeType = cursor.getString(mimeColumn),
                )
            }
        }
        rows
    }
}
