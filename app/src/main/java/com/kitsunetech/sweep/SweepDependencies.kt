package com.kitsunetech.sweep

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kitsunetech.sweep.data.apps.AndroidInstalledAppsSource
import com.kitsunetech.sweep.data.apps.AndroidStorageFactsSource
import com.kitsunetech.sweep.data.apps.AndroidUsageFactsSource
import com.kitsunetech.sweep.data.apps.DefaultAppInventoryRepository
import com.kitsunetech.sweep.data.storage.AndroidContentStreamOpener
import com.kitsunetech.sweep.data.storage.DirectStorageScanner
import com.kitsunetech.sweep.data.storage.FileDeletionCoordinator
import com.kitsunetech.sweep.data.system.PermissionStateReader
import com.kitsunetech.sweep.domain.FileSafetyPolicy
import com.kitsunetech.sweep.domain.DuplicateDetector
import com.kitsunetech.sweep.domain.Sha256ContentHasher
import com.kitsunetech.sweep.ui.StorageSummary
import com.kitsunetech.sweep.ui.StorageSummarySource
import com.kitsunetech.sweep.ui.SweepViewModel
import java.nio.file.Path

class SweepDependencies(context: Context) {
    private val applicationContext = context.applicationContext
    private val sharedRoots: Set<Path> = setOf(Environment.getExternalStorageDirectory().toPath())
    private val appInventory = DefaultAppInventoryRepository(
        installedAppsSource = AndroidInstalledAppsSource(applicationContext),
        usageFactsSource = AndroidUsageFactsSource(applicationContext),
        storageFactsSource = AndroidStorageFactsSource(applicationContext),
    )

    val deletionCoordinator = FileDeletionCoordinator(
        context = applicationContext,
        roots = sharedRoots,
        safetyPolicy = FileSafetyPolicy(
            protectedRoots = setOf(
                applicationContext.filesDir.toPath(),
                applicationContext.cacheDir.toPath(),
                applicationContext.dataDir.toPath(),
            ),
        ),
    )

    val viewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SweepViewModel::class.java))
            return SweepViewModel(
                permissionStateSource = PermissionStateReader(applicationContext),
                storageSummarySource = AndroidStorageSummarySource(),
                directScanner = DirectStorageScanner(sharedRoots),
                duplicateDetector = DuplicateDetector(
                    Sha256ContentHasher(AndroidContentStreamOpener(applicationContext)),
                ),
                appInventoryRepository = appInventory,
            ) as T
        }
    }
}

private class AndroidStorageSummarySource : StorageSummarySource {
    override suspend fun read(): StorageSummary {
        val stats = StatFs(Environment.getDataDirectory().absolutePath)
        return StorageSummary(
            totalBytes = stats.totalBytes.coerceAtLeast(0L),
            freeBytes = stats.availableBytes.coerceAtLeast(0L),
        )
    }
}
