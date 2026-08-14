package com.kitsunetech.sweep.ui

import com.kitsunetech.sweep.data.apps.AppProgress
import com.kitsunetech.sweep.data.storage.ScanProgress
import com.kitsunetech.sweep.data.system.PermissionState
import com.kitsunetech.sweep.domain.AppRecord
import com.kitsunetech.sweep.domain.DuplicateGroup
import com.kitsunetech.sweep.domain.HashProgress
import com.kitsunetech.sweep.domain.StorageFile

data class StorageSummary(
    val totalBytes: Long,
    val freeBytes: Long,
)

fun interface StorageSummarySource {
    suspend fun read(): StorageSummary
}

data class HomeState(
    val permissions: PermissionState = PermissionState(false, false),
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val error: String? = null,
)

data class FilesState(
    val minBytes: Long = 100L * 1024L * 1024L,
    val files: List<StorageFile> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val progress: ScanProgress? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val selectedBytes: Long
        get() = files.asSequence()
            .filter { it.id in selectedIds }
            .sumOf { it.sizeBytes.coerceAtLeast(0L) }
}

data class DuplicatesState(
    val groups: List<DuplicateGroup> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val progress: HashProgress? = null,
    val isLoading: Boolean = false,
    val skippedFiles: Int = 0,
    val error: String? = null,
)

data class AppsState(
    val apps: List<AppRecord> = emptyList(),
    val sort: AppSort = AppSort.TOTAL_SIZE,
    val progress: AppProgress? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class DeletionNotice(
    val deletedCount: Int,
    val remainingLocations: List<String>,
)

data class SweepUiState(
    val destination: SweepDestination = SweepDestination.HOME,
    val home: HomeState = HomeState(),
    val files: FilesState = FilesState(),
    val duplicates: DuplicatesState = DuplicatesState(),
    val apps: AppsState = AppsState(),
    val deletionNotice: DeletionNotice? = null,
)
