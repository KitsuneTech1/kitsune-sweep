package com.kitsunetech.sweep.data.apps

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAppSourcesInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun installedAppsIncludeTheRunningPackage() = runBlocking {
        val apps = AndroidInstalledAppsSource(context).load()

        assertTrue(apps.any { it.packageName == context.packageName && it.label.isNotBlank() })
    }

    @Test
    fun usageSourceReturnsWithoutSpecialAccess() = runBlocking {
        val usage = AndroidUsageFactsSource(context).load()

        assertNotNull(usage)
    }

    @Test
    fun storageSourceCanReadTheRunningPackage() = runBlocking {
        val fact = AndroidStorageFactsSource(context).load(context.packageName)

        assertNotNull(fact)
        assertTrue(fact!!.appBytes >= 0L)
        assertTrue(fact.dataBytes >= 0L)
        assertTrue(fact.cacheBytes >= 0L)
    }
}
