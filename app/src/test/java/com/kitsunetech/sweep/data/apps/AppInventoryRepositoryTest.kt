package com.kitsunetech.sweep.data.apps

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class AppInventoryRepositoryTest {
    @Test
    fun mergesOptionalFactsAndSortsLabelsWithoutLosingSystemStatus() {
        val now = 1_800_000_000_000L
        val installed = listOf(
            InstalledAppFact("z.package", "zebra", 100L, isSystem = false),
            InstalledAppFact("a.package", "Alpha", 200L, isSystem = true),
        )
        val usage = mapOf("z.package" to UsageFact("z.package", now - 100L * DAY))
        val storage = mapOf("z.package" to StorageFact("z.package", 10L, 20L, 30L))

        val apps = mergeAppFacts(installed, usage, storage, now)

        assertEquals(listOf("Alpha", "zebra"), apps.map { it.label })
        assertTrue(apps.first().isSystem)
        assertFalse(apps.first().isCold)
        assertNull(apps.first().appBytes)
        assertNull(apps.first().lastUsedAtMillis)

        val zebra = apps.last()
        assertEquals(10L, zebra.appBytes)
        assertEquals(20L, zebra.dataBytes)
        assertEquals(30L, zebra.cacheBytes)
        assertEquals(30L, zebra.totalBytes)
        assertTrue(zebra.isCold)
    }

    @Test
    fun repositoryKeepsLoadingWhenOneStorageLookupFails() = runBlocking {
        val now = 1_800_000_000_000L
        val installedSource = InstalledAppsSource {
            listOf(
                InstalledAppFact("good.package", "Good", 100L, isSystem = false),
                InstalledAppFact("blocked.package", "Blocked", 200L, isSystem = false),
            )
        }
        val usageSource = UsageFactsSource {
            mapOf("good.package" to UsageFact("good.package", now - DAY))
        }
        val storageSource = StorageFactsSource { packageName ->
            if (packageName == "blocked.package") throw SecurityException("denied")
            StorageFact(packageName, 10L, 20L, 30L)
        }
        val progress = mutableListOf<AppProgress>()
        val repository = DefaultAppInventoryRepository(
            installedAppsSource = installedSource,
            usageFactsSource = usageSource,
            storageFactsSource = storageSource,
            nowMillis = { now },
        )

        val apps = repository.loadApps { progress += it }

        assertEquals(2, apps.size)
        assertNull(apps.first { it.packageName == "blocked.package" }.totalBytes)
        assertEquals(30L, apps.first { it.packageName == "good.package" }.totalBytes)
        assertEquals(AppProgress(completed = 2, total = 2), progress.last())
    }

    @Test
    fun repositoryDoesNotSwallowCancellation() {
        val repository = DefaultAppInventoryRepository(
            installedAppsSource = InstalledAppsSource {
                listOf(InstalledAppFact("one.package", "One", 100L, isSystem = false))
            },
            usageFactsSource = UsageFactsSource { emptyMap() },
            storageFactsSource = StorageFactsSource { throw CancellationException("cancelled") },
        )

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.loadApps {} }
        }
    }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
    }
}
