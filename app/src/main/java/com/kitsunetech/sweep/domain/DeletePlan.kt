package com.kitsunetech.sweep.domain

data class DeletePlan(
    val files: List<StorageFile>,
    val totalBytes: Long,
)

fun buildDeletePlan(selected: Collection<StorageFile>): DeletePlan {
    val files = selected
        .distinctBy { it.id }
        .sortedByDescending { it.sizeBytes.coerceAtLeast(0L) }
    return DeletePlan(
        files = files,
        totalBytes = files.sumOf { it.sizeBytes.coerceAtLeast(0L) },
    )
}
