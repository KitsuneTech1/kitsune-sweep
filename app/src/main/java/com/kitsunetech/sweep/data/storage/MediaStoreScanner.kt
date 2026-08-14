package com.kitsunetech.sweep.data.storage

import com.kitsunetech.sweep.domain.FileSource
import com.kitsunetech.sweep.domain.StorageFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class MediaStoreRecord(
    val contentUri: String,
    val displayName: String?,
    val sizeBytes: Long,
    val modifiedAtSeconds: Long,
    val mimeType: String?,
)

class MediaStoreScanner(
    private val loadRecords: suspend () -> List<MediaStoreRecord>,
) : StorageScanner {
    override suspend fun scanLargeFiles(
        minBytes: Long,
        onProgress: (ScanProgress) -> Unit,
    ): ScanResult {
        val context = currentCoroutineContext()
        val threshold = minBytes.coerceAtLeast(0L)
        val matches = mutableListOf<StorageFile>()
        var visited = 0L

        loadRecords().forEach { record ->
            context.ensureActive()
            visited += 1
            if (record.sizeBytes >= threshold) {
                matches += StorageFile(
                    id = record.contentUri,
                    displayName = record.displayName?.takeIf { it.isNotBlank() } ?: "Unknown file",
                    path = null,
                    contentUri = record.contentUri,
                    sizeBytes = record.sizeBytes,
                    modifiedAtMillis = record.modifiedAtSeconds.coerceAtLeast(0L) * 1000L,
                    mimeType = record.mimeType,
                    source = FileSource.MEDIA_STORE,
                )
            }
            if (visited % PROGRESS_INTERVAL == 0L) {
                onProgress(ScanProgress(visited, matches.size))
            }
        }

        onProgress(ScanProgress(visited, matches.size))
        return ScanResult(
            files = matches.sortedByDescending { it.sizeBytes },
            skipped = 0,
            errors = emptyList(),
        )
    }

    private companion object {
        const val PROGRESS_INTERVAL = 128L
    }
}
