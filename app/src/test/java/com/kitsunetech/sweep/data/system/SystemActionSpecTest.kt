package com.kitsunetech.sweep.data.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SystemActionSpecTest {
    @Test
    fun mapsSettingsAndPackageActionsToExactDescriptors() {
        assertEquals(
            IntentDescriptor(
                action = "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
                dataUri = "package:com.kitsunetech.sweep",
            ),
            SystemActionSpec.RequestAllFiles.toDescriptor("com.kitsunetech.sweep"),
        )
        assertEquals(
            IntentDescriptor(
                action = "android.settings.APPLICATION_DETAILS_SETTINGS",
                dataUri = "package:com.example.player",
            ),
            SystemActionSpec.AppDetails("com.example.player").toDescriptor("com.kitsunetech.sweep"),
        )
        assertEquals(
            IntentDescriptor(
                action = "android.intent.action.DELETE",
                dataUri = "package:com.example.player",
            ),
            SystemActionSpec.Uninstall("com.example.player").toDescriptor("com.kitsunetech.sweep"),
        )
    }

    @Test
    fun rejectsInvalidPackageNamesBeforeCreatingDescriptor() {
        listOf("", "single", "has space.app", "slash/app", ".leading.dot").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                SystemActionSpec.AppDetails(invalid).toDescriptor("com.kitsunetech.sweep")
            }
        }
    }

    @Test
    fun mapsStorageAndCacheActionsWithoutInventedUris() {
        assertEquals(
            IntentDescriptor("android.settings.INTERNAL_STORAGE_SETTINGS", null),
            SystemActionSpec.ManageStorage.toDescriptor("com.kitsunetech.sweep"),
        )
        assertEquals(
            IntentDescriptor("android.os.storage.action.CLEAR_APP_CACHE", null),
            SystemActionSpec.ClearExternalCaches.toDescriptor("com.kitsunetech.sweep"),
        )
    }
}
