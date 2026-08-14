package com.kitsunetech.sweep.data.storage

import com.kitsunetech.sweep.domain.StorageFile

data class ScanProgress(
    val visited: Long,
    val matched: Int,
)

data class ScanResult(
    val files: List<StorageFile>,
    val skipped: Int,
    val errors: List<String>,
)

fun interface StorageScanner {
    suspend fun scanLargeFiles(
        minBytes: Long,
        onProgress: (ScanProgress) -> Unit,
    ): ScanResult
}

