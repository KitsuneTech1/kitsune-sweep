package com.kitsunetech.sweep.data.system

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemActionsInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun requiredSettingsActionsResolveOnApi34() {
        val actions = listOf(
            SystemActionSpec.RequestAllFiles,
            SystemActionSpec.RequestUsage,
            SystemActionSpec.ManageStorage,
            SystemActionSpec.ClearExternalCaches,
            SystemActionSpec.AppDetails(context.packageName),
        )

        actions.forEach { action ->
            val intent = action.toIntent(context.packageName)
            assertNotNull(
                action.toString(),
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY),
            )
        }
    }

    @Test
    fun permissionReaderReturnsCurrentStateWithoutPrompting() {
        val state = PermissionStateReader(context).read()

        assertNotNull(state)
    }
}
