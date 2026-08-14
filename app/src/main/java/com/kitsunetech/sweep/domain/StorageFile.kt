package com.kitsunetech.sweep.domain

enum class FileSource {
    DIRECT,
    MEDIA_STORE,
}

data class StorageFile(
    val id: String,
    val displayName: String,
    val path: String?,
    val contentUri: String?,
    val sizeBytes: Long,
    val modifiedAtMillis: Long,
    val mimeType: String?,
    val source: FileSource,
)

