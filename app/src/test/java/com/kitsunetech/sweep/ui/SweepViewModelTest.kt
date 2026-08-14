package com.kitsunetech.sweep.ui

import com.kitsunetech.sweep.data.apps.AppInventoryRepository
import com.kitsunetech.sweep.data.apps.AppProgress
import com.kitsunetech.sweep.data.storage.ScanProgress
import com.kitsunetech.sweep.data.storage.ScanResult
import com.kitsunetech.sweep.data.storage.StorageScanner
import com.kitsunetech.sweep.data.storage.DeletionOutcome
import com.kitsunetech.sweep.data.storage.DeletionRequest
import com.kitsunetech.sweep.domain.buildDeletePlan
import com.kitsunetech.sweep.data.system.PermissionState
import com.kitsunetech.sweep.data.system.PermissionStateSource
import com.kitsunetech.sweep.domain.AppRecord
import com.kitsunetech.sweep.domain.ContentHasher
import com.kitsunetech.sweep.domain.DuplicateDetector
import com.kitsunetech.sweep.domain.FileSource
import com.kitsunetech.sweep.domain.StorageFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SweepViewModelTest {
    @Test
    fun refreshesPermissionsAndStorageSummary() = runTest {
        val viewModel = viewModel(
            permissionState = PermissionState(allFilesAccess = true, usageAccess = false),
            summary = StorageSummary(totalBytes = 1_000L, freeBytes = 400L),
        )

        viewModel.refreshPermissions()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.home.permissions.allFilesAccess)
        assertFalse(viewModel.state.value.home.permissions.usageAccess)
        assertEquals(1_000L, viewModel.state.value.home.totalBytes)
        assertEquals(400L, viewModel.state.value.home.freeBytes)
    }

    @Test
    fun requiresAllFilesAccessBeforeScanningSharedFiles() = runTest {
        val direct = RecordingScanner(emptyList())
        val viewModel = viewModel(
            permissionState = PermissionState(allFilesAccess = false, usageAccess = false),
            directScanner = direct,
        )

        viewModel.refreshPermissions()
        viewModel.scanLargeFiles(100L)
        advanceUntilIdle()
        assertTrue(direct.thresholds.isEmpty())
        assertTrue(viewModel.state.value.files.files.isEmpty())
        assertEquals("All Files Access is required to scan shared files.", viewModel.state.value.files.error)
    }

    @Test
    fun changingThresholdCancelsObsoleteScan() = runTest {
        val scanner = CancellableScanner()
        val viewModel = viewModel(
            permissionState = PermissionState(allFilesAccess = true, usageAccess = false),
            directScanner = scanner,
        )

        viewModel.refreshPermissions()
        viewModel.scanLargeFiles(50L)
        scanner.firstStarted.await()
        viewModel.scanLargeFiles(100L)
        advanceUntilIdle()

        assertTrue(scanner.firstCancelled)
        assertEquals(100L, viewModel.state.value.files.minBytes)
        assertEquals(listOf("fresh"), viewModel.state.value.files.files.map { it.id })
    }

    @Test
    fun obsoleteNonCooperativeFileScanCannotReplaceNewerResults() = runTest {
        val scanner = OutOfOrderScanner()
        val viewModel = viewModel(directScanner = scanner)

        viewModel.refreshPermissions()
        viewModel.scanLargeFiles(50L)
        scanner.firstStarted.await()
        viewModel.scanLargeFiles(100L)
        advanceUntilIdle()
        scanner.releaseFirst.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("fresh"), viewModel.state.value.files.files.map { it.id })
        assertEquals(100L, viewModel.state.value.files.minBytes)
    }

    @Test
    fun newFileScanCancelsDuplicatePassAndBlocksStaleGroups() = runTest {
        val scanner = SequencedScanner(
            listOf(file("old-a", 200L), file("old-b", 200L)),
            listOf(file("fresh", 300L)),
        )
        val hasher = OutOfOrderHasher()
        val viewModel = viewModel(directScanner = scanner, hasher = hasher)

        viewModel.refreshPermissions()
        viewModel.scanLargeFiles(1L)
        advanceUntilIdle()
        viewModel.scanDuplicates()
        hasher.started.await()
        viewModel.scanLargeFiles(1L)
        advanceUntilIdle()
        hasher.release.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("fresh"), viewModel.state.value.files.files.map { it.id })
        assertTrue(viewModel.state.value.duplicates.groups.isEmpty())
        assertFalse(viewModel.state.value.duplicates.isLoading)
    }

    @Test
    fun leavingFilesCancelsItsActiveScan() = runTest {
        val scanner = CancellableScanner()
        val viewModel = viewModel(directScanner = scanner)

        viewModel.refreshPermissions()
        viewModel.selectDestination(SweepDestination.FILES)
        viewModel.scanLargeFiles(50L)
        scanner.firstStarted.await()
        viewModel.selectDestination(SweepDestination.HOME)
        advanceUntilIdle()

        assertTrue(scanner.firstCancelled)
        assertFalse(viewModel.state.value.files.isLoading)
        assertEquals(SweepDestination.HOME, viewModel.state.value.destination)
    }

    @Test
    fun findsDuplicatesFromLatestFileResult() = runTest {
        val files = listOf(file("a", 200L), file("b", 200L), file("unique", 50L))
        val viewModel = viewModel(
            directScanner = RecordingScanner(files),
            hasher = ContentHasher { item -> if (item.id == "unique") "unique" else "same" },
        )

        viewModel.refreshPermissions()
        viewModel.scanLargeFiles(1L)
        advanceUntilIdle()
        viewModel.scanDuplicates()
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.duplicates.groups.size)
        assertEquals(200L, viewModel.state.value.duplicates.groups.single().reclaimableBytes)
    }

    @Test
    fun sortsAppsBySizeCacheAndOldestUse() = runTest {
        val apps = listOf(
            app("small-old", total = 50L, cache = 10L, lastUse = 100L),
            app("large-new", total = 500L, cache = 20L, lastUse = 500L),
            app("cache-heavy", total = 200L, cache = 100L, lastUse = 300L),
        )
        val viewModel = viewModel(apps = apps)

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals("large-new", viewModel.state.value.apps.apps.first().packageName)

        viewModel.sortApps(AppSort.CACHE)
        assertEquals("cache-heavy", viewModel.state.value.apps.apps.first().packageName)

        viewModel.sortApps(AppSort.OLDEST_USE)
        assertEquals("small-old", viewModel.state.value.apps.apps.first().packageName)
    }

    @Test
    fun retainsPendingDeletionAcrossActivityRecreationAndReportsRemainingPaths() = runTest {
        val viewModel = viewModel()
        val selected = file("selected", 100L)
        val request = DeletionRequest(
            directPlan = buildDeletePlan(listOf(selected)),
            mediaStorePlan = buildDeletePlan(emptyList()),
            mediaStoreIntentSender = null,
            unsupportedFiles = emptyList(),
        )

        viewModel.retainPendingDeletion(request)
        assertTrue(viewModel.hasPendingDeletion())
        assertTrue(
            viewModel.resolvePendingDeletion(approved = true) {
                DeletionOutcome(deletedCount = 0, remainingLocations = listOf(selected.path!!))
            },
        )
        advanceUntilIdle()

        assertFalse(viewModel.hasPendingDeletion())
        assertEquals(listOf(selected.path), viewModel.state.value.deletionNotice?.remainingLocations)
        viewModel.dismissDeletionNotice()
        assertEquals(null, viewModel.state.value.deletionNotice)
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        permissionState: PermissionState = PermissionState(true, true),
        summary: StorageSummary = StorageSummary(1_000L, 500L),
        directScanner: StorageScanner = RecordingScanner(emptyList()),
        hasher: ContentHasher = ContentHasher { it.id },
        apps: List<AppRecord> = emptyList(),
    ) = SweepViewModel(
        permissionStateSource = PermissionStateSource { permissionState },
        storageSummarySource = StorageSummarySource { summary },
        directScanner = directScanner,
        duplicateDetector = DuplicateDetector(hasher),
        appInventoryRepository = AppInventoryRepository { progress ->
            progress(AppProgress(apps.size, apps.size))
            apps
        },
        ioDispatcher = StandardTestDispatcher(testScheduler),
        externalScope = this,
    )

    private class RecordingScanner(
        private val files: List<StorageFile>,
    ) : StorageScanner {
        val thresholds = mutableListOf<Long>()

        override suspend fun scanLargeFiles(
            minBytes: Long,
            onProgress: (ScanProgress) -> Unit,
        ): ScanResult {
            thresholds += minBytes
            onProgress(ScanProgress(files.size.toLong(), files.size))
            return ScanResult(files, skipped = 0, errors = emptyList())
        }
    }

    private class CancellableScanner : StorageScanner {
        val firstStarted = CompletableDeferred<Unit>()
        var firstCancelled = false

        override suspend fun scanLargeFiles(
            minBytes: Long,
            onProgress: (ScanProgress) -> Unit,
        ): ScanResult {
            if (minBytes == 50L) {
                firstStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    firstCancelled = true
                }
            }
            return ScanResult(listOf(file("fresh", 100L)), skipped = 0, errors = emptyList())
        }
    }

    private class OutOfOrderScanner : StorageScanner {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        override suspend fun scanLargeFiles(
            minBytes: Long,
            onProgress: (ScanProgress) -> Unit,
        ): ScanResult {
            if (minBytes == 50L) {
                firstStarted.complete(Unit)
                withContext(NonCancellable) { releaseFirst.await() }
                return ScanResult(listOf(file("stale", 50L)), 0, emptyList())
            }
            return ScanResult(listOf(file("fresh", 100L)), 0, emptyList())
        }
    }

    private class SequencedScanner(
        private vararg val results: List<StorageFile>,
    ) : StorageScanner {
        private var index = 0

        override suspend fun scanLargeFiles(
            minBytes: Long,
            onProgress: (ScanProgress) -> Unit,
        ): ScanResult {
            val files = results[index.coerceAtMost(results.lastIndex)]
            index += 1
            return ScanResult(files, 0, emptyList())
        }
    }

    private class OutOfOrderHasher : ContentHasher {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        override suspend fun sha256(file: StorageFile): String {
            started.complete(Unit)
            withContext(NonCancellable) { release.await() }
            return "stale-hash"
        }
    }

    private fun app(
        packageName: String,
        total: Long,
        cache: Long,
        lastUse: Long,
    ) = AppRecord(
        packageName = packageName,
        label = packageName,
        appBytes = total - cache,
        dataBytes = 0L,
        cacheBytes = cache,
        lastUsedAtMillis = lastUse,
        firstInstalledAtMillis = 0L,
        isSystem = false,
        isCold = false,
    )

    private companion object {
        fun file(id: String, size: Long) = StorageFile(
            id = id,
            displayName = "$id.bin",
            path = "/storage/$id.bin",
            contentUri = null,
            sizeBytes = size,
            modifiedAtMillis = 0L,
            mimeType = null,
            source = FileSource.DIRECT,
        )
    }
}
