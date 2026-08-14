package com.kitsunetech.sweep.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitsunetech.sweep.data.apps.AppInventoryRepository
import com.kitsunetech.sweep.data.apps.AppProgress
import com.kitsunetech.sweep.data.storage.ScanProgress
import com.kitsunetech.sweep.data.storage.StorageScanner
import com.kitsunetech.sweep.data.system.PermissionStateSource
import com.kitsunetech.sweep.domain.AppRecord
import com.kitsunetech.sweep.domain.DuplicateDetector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SweepViewModel(
    private val permissionStateSource: PermissionStateSource,
    private val storageSummarySource: StorageSummarySource,
    private val directScanner: StorageScanner,
    private val mediaStoreScanner: StorageScanner,
    private val duplicateDetector: DuplicateDetector,
    private val appInventoryRepository: AppInventoryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = externalScope ?: viewModelScope
    private val mutableState = MutableStateFlow(SweepUiState())
    val state: StateFlow<SweepUiState> = mutableState.asStateFlow()

    private var fileJob: Job? = null
    private var duplicateJob: Job? = null
    private var appJob: Job? = null

    fun selectDestination(destination: SweepDestination) {
        mutableState.update { it.copy(destination = destination) }
    }

    fun refreshPermissions() {
        val permissions = permissionStateSource.read()
        mutableState.update { current ->
            current.copy(home = current.home.copy(permissions = permissions, error = null))
        }
        scope.launch(ioDispatcher) {
            try {
                val summary = storageSummarySource.read()
                mutableState.update { current ->
                    current.copy(
                        home = current.home.copy(
                            totalBytes = summary.totalBytes.coerceAtLeast(0L),
                            freeBytes = summary.freeBytes.coerceAtLeast(0L),
                            error = null,
                        ),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { current ->
                    current.copy(home = current.home.copy(error = "Storage totals are unavailable."))
                }
            }
        }
    }

    fun scanLargeFiles(minBytes: Long) {
        fileJob?.cancel()
        val threshold = minBytes.coerceAtLeast(0L)
        val scanner = if (state.value.home.permissions.allFilesAccess) directScanner else mediaStoreScanner
        fileJob = scope.launch(ioDispatcher) {
            mutableState.update { current ->
                current.copy(
                    files = current.files.copy(
                        minBytes = threshold,
                        selectedIds = emptySet(),
                        progress = ScanProgress(0L, 0),
                        isLoading = true,
                        error = null,
                    ),
                )
            }
            try {
                val result = scanner.scanLargeFiles(threshold) { progress ->
                    mutableState.update { current ->
                        current.copy(files = current.files.copy(progress = progress))
                    }
                }
                val partialError = result.errors.takeIf { it.isNotEmpty() }
                    ?.let { "${it.size} storage locations could not be read." }
                mutableState.update { current ->
                    current.copy(
                        files = current.files.copy(
                            files = result.files,
                            isLoading = false,
                            error = partialError,
                        ),
                        duplicates = DuplicatesState(),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { current ->
                    current.copy(
                        files = current.files.copy(
                            isLoading = false,
                            error = "Android could not finish the file scan.",
                        ),
                    )
                }
            }
        }
    }

    fun toggleFile(id: String) {
        mutableState.update { current ->
            val selected = current.files.selectedIds.toMutableSet()
            if (!selected.add(id)) selected.remove(id)
            current.copy(files = current.files.copy(selectedIds = selected))
        }
    }

    fun clearFileSelection() {
        mutableState.update { current ->
            current.copy(files = current.files.copy(selectedIds = emptySet()))
        }
    }

    fun scanDuplicates() {
        duplicateJob?.cancel()
        val sourceFiles = state.value.files.files
        duplicateJob = scope.launch(ioDispatcher) {
            mutableState.update { current ->
                current.copy(
                    duplicates = current.duplicates.copy(
                        isLoading = true,
                        progress = null,
                        error = null,
                        selectedIds = emptySet(),
                    ),
                )
            }
            try {
                val groups = duplicateDetector.findExact(sourceFiles) { progress ->
                    mutableState.update { current ->
                        current.copy(duplicates = current.duplicates.copy(progress = progress))
                    }
                }
                mutableState.update { current ->
                    current.copy(duplicates = current.duplicates.copy(groups = groups, isLoading = false))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { current ->
                    current.copy(
                        duplicates = current.duplicates.copy(
                            isLoading = false,
                            error = "Android could not finish checking duplicates.",
                        ),
                    )
                }
            }
        }
    }

    fun toggleDuplicateFile(id: String) {
        mutableState.update { current ->
            val selected = current.duplicates.selectedIds.toMutableSet()
            if (!selected.add(id)) selected.remove(id)
            current.copy(duplicates = current.duplicates.copy(selectedIds = selected))
        }
    }

    fun loadApps() {
        appJob?.cancel()
        appJob = scope.launch(ioDispatcher) {
            mutableState.update { current ->
                current.copy(
                    apps = current.apps.copy(
                        isLoading = true,
                        progress = AppProgress(0, 0),
                        error = null,
                    ),
                )
            }
            try {
                val loaded = appInventoryRepository.loadApps { progress ->
                    mutableState.update { current ->
                        current.copy(apps = current.apps.copy(progress = progress))
                    }
                }
                mutableState.update { current ->
                    current.copy(
                        apps = current.apps.copy(
                            apps = sortApps(loaded, current.apps.sort),
                            isLoading = false,
                        ),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { current ->
                    current.copy(
                        apps = current.apps.copy(
                            isLoading = false,
                            error = "Android could not finish reading app storage.",
                        ),
                    )
                }
            }
        }
    }

    fun sortApps(sort: AppSort) {
        mutableState.update { current ->
            current.copy(apps = current.apps.copy(apps = sortApps(current.apps.apps, sort), sort = sort))
        }
    }

    private fun sortApps(apps: List<AppRecord>, sort: AppSort): List<AppRecord> = when (sort) {
        AppSort.TOTAL_SIZE -> apps.sortedByDescending { it.totalBytes ?: -1L }
        AppSort.CACHE -> apps.sortedByDescending { it.cacheBytes ?: -1L }
        AppSort.OLDEST_USE -> apps.sortedBy { it.lastUsedAtMillis ?: Long.MAX_VALUE }
    }
}
