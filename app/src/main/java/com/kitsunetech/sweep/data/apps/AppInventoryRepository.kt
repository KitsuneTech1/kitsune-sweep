package com.kitsunetech.sweep.data.apps

import com.kitsunetech.sweep.domain.AppRecord
import com.kitsunetech.sweep.domain.classifyColdApp

data class InstalledAppFact(
    val packageName: String,
    val label: String,
    val firstInstalledAtMillis: Long,
    val isSystem: Boolean,
)

data class UsageFact(
    val packageName: String,
    val lastUsedAtMillis: Long,
)

data class StorageFact(
    val packageName: String,
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
)

data class AppProgress(
    val completed: Int,
    val total: Int,
)

fun interface InstalledAppsSource {
    suspend fun load(): List<InstalledAppFact>
}

fun interface UsageFactsSource {
    suspend fun load(): Map<String, UsageFact>
}

fun interface StorageFactsSource {
    suspend fun load(packageName: String): StorageFact?
}

fun interface AppInventoryRepository {
    suspend fun loadApps(onProgress: (AppProgress) -> Unit): List<AppRecord>
}

class DefaultAppInventoryRepository(
    private val installedAppsSource: InstalledAppsSource,
    private val usageFactsSource: UsageFactsSource,
    private val storageFactsSource: StorageFactsSource,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : AppInventoryRepository {
    override suspend fun loadApps(onProgress: (AppProgress) -> Unit): List<AppRecord> {
        val installed = installedAppsSource.load()
        val usage = usageFactsSource.load()
        val storage = buildMap {
            installed.forEachIndexed { index, app ->
                val fact = try {
                    storageFactsSource.load(app.packageName)
                } catch (_: SecurityException) {
                    null
                } catch (_: Exception) {
                    null
                }
                if (fact != null) put(app.packageName, fact)
                onProgress(AppProgress(index + 1, installed.size))
            }
        }
        return mergeAppFacts(installed, usage, storage, nowMillis())
    }
}

fun mergeAppFacts(
    installed: List<InstalledAppFact>,
    usage: Map<String, UsageFact>,
    storage: Map<String, StorageFact>,
    nowMillis: Long,
): List<AppRecord> = installed
    .map { app ->
        val usageFact = usage[app.packageName]
        val storageFact = storage[app.packageName]
        AppRecord(
            packageName = app.packageName,
            label = app.label,
            appBytes = storageFact?.appBytes,
            dataBytes = storageFact?.dataBytes,
            cacheBytes = storageFact?.cacheBytes,
            lastUsedAtMillis = usageFact?.lastUsedAtMillis?.takeIf { it > 0L },
            firstInstalledAtMillis = app.firstInstalledAtMillis,
            isSystem = app.isSystem,
            isCold = classifyColdApp(
                isSystem = app.isSystem,
                lastUsedAtMillis = usageFact?.lastUsedAtMillis,
                nowMillis = nowMillis,
            ),
        )
    }
    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
