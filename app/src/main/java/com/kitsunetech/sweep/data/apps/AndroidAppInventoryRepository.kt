package com.kitsunetech.sweep.data.apps

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidInstalledAppsSource(
    context: Context,
) : InstalledAppsSource {
    private val packageManager = context.applicationContext.packageManager

    override suspend fun load(): List<InstalledAppFact> = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        packageManager.getInstalledApplications(0)
            .mapNotNull { applicationInfo -> applicationInfo.toInstalledFact() }
    }

    private fun ApplicationInfo.toInstalledFact(): InstalledAppFact? {
        val packageInfo = runCatching {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }.getOrNull() ?: return null
        val systemMask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        return InstalledAppFact(
            packageName = packageName,
            label = runCatching { packageManager.getApplicationLabel(this).toString() }
                .getOrDefault(packageName),
            firstInstalledAtMillis = packageInfo.firstInstallTime.coerceAtLeast(0L),
            isSystem = flags and systemMask != 0,
        )
    }
}

class AndroidUsageFactsSource(
    context: Context,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : UsageFactsSource {
    private val usageStatsManager = context.applicationContext
        .getSystemService(UsageStatsManager::class.java)

    override suspend fun load(): Map<String, UsageFact> = withContext(Dispatchers.IO) {
        val end = nowMillis()
        val begin = end - LOOKBACK_MILLIS
        runCatching { usageStatsManager.queryAndAggregateUsageStats(begin, end) }
            .getOrDefault(emptyMap())
            .mapValues { (packageName, stats) ->
                UsageFact(packageName, stats.lastTimeUsed.coerceAtLeast(0L))
            }
    }

    private companion object {
        const val LOOKBACK_MILLIS = 365L * 24L * 60L * 60L * 1000L
    }
}

class AndroidStorageFactsSource(
    context: Context,
) : StorageFactsSource {
    private val storageStatsManager = context.applicationContext
        .getSystemService(StorageStatsManager::class.java)

    override suspend fun load(packageName: String): StorageFact = withContext(Dispatchers.IO) {
        val stats = storageStatsManager.queryStatsForPackage(
            StorageManager.UUID_DEFAULT,
            packageName,
            Process.myUserHandle(),
        )
        StorageFact(
            packageName = packageName,
            appBytes = stats.appBytes.coerceAtLeast(0L),
            dataBytes = stats.dataBytes.coerceAtLeast(0L),
            cacheBytes = stats.cacheBytes.coerceAtLeast(0L),
        )
    }
}
