package com.kitsunetech.sweep

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kitsunetech.sweep.data.system.SystemActionSpec
import com.kitsunetech.sweep.data.system.resolveIntent
import com.kitsunetech.sweep.domain.DeletePlan
import com.kitsunetech.sweep.ui.SweepActions
import com.kitsunetech.sweep.ui.SweepApp
import com.kitsunetech.sweep.ui.SweepViewModel
import com.kitsunetech.sweep.ui.theme.SweepTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val dependencies by lazy { SweepDependencies(applicationContext) }
    private val viewModel by viewModels<SweepViewModel> { dependencies.viewModelFactory }
    private var directPlanAfterMediaApproval: DeletePlan? = null

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshPermissions()
    }

    private val mediaDeletionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val directPlan = directPlanAfterMediaApproval
        directPlanAfterMediaApproval = null
        if (result.resultCode == Activity.RESULT_OK) {
            if (directPlan != null && directPlan.files.isNotEmpty()) {
                deleteDirectFiles(directPlan)
            } else {
                refreshFileScan()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            SweepTheme {
                SweepApp(
                    state = state,
                    actions = SweepActions(
                        onNavigate = viewModel::selectDestination,
                        onRequestAllFiles = { launchSystemAction(SystemActionSpec.RequestAllFiles) },
                        onRequestUsage = { launchSystemAction(SystemActionSpec.RequestUsage) },
                        onOpenStorageTools = { launchSystemAction(SystemActionSpec.ManageStorage) },
                        onClearCaches = { launchSystemAction(SystemActionSpec.ClearExternalCaches) },
                        onScanLargeFiles = viewModel::scanLargeFiles,
                        onToggleFile = viewModel::toggleFile,
                        onScanDuplicates = viewModel::scanDuplicates,
                        onToggleDuplicateFile = viewModel::toggleDuplicateFile,
                        onLoadApps = viewModel::loadApps,
                        onSortApps = viewModel::sortApps,
                        onAppDetails = { launchSystemAction(SystemActionSpec.AppDetails(it)) },
                        onUninstall = { launchSystemAction(SystemActionSpec.Uninstall(it)) },
                        onConfirmDeletion = ::requestDeletion,
                    ),
                )
            }
        }
        viewModel.refreshPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    private fun launchSystemAction(spec: SystemActionSpec) {
        val intent = spec.resolveIntent(this)
        if (intent == null) {
            Toast.makeText(this, "Android could not open that setting.", Toast.LENGTH_SHORT).show()
            return
        }
        settingsLauncher.launch(intent.withActivityFlag())
    }

    private fun requestDeletion(plan: DeletePlan) {
        val request = runCatching { dependencies.deletionCoordinator.createRequest(plan) }
            .getOrElse {
                Toast.makeText(this, "Android could not prepare that deletion.", Toast.LENGTH_SHORT).show()
                return
            }
        if (request.unsupportedIds.isNotEmpty()) {
            Toast.makeText(
                this,
                "${request.unsupportedIds.size} files need to be reviewed again.",
                Toast.LENGTH_SHORT,
            ).show()
        }
        val sender = request.mediaStoreIntentSender
        if (sender != null) {
            directPlanAfterMediaApproval = request.directPlan
            mediaDeletionLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } else if (request.directPlan.files.isNotEmpty()) {
            deleteDirectFiles(request.directPlan)
        }
    }

    private fun deleteDirectFiles(plan: DeletePlan) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                dependencies.deletionCoordinator.deleteConfirmedDirectFiles(plan)
            }
            val message = if (result.failedIds.isEmpty()) {
                "Deleted ${result.deletedIds.size} files."
            } else {
                "Deleted ${result.deletedIds.size} files. ${result.failedIds.size} need review."
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            refreshFileScan()
        }
    }

    private fun refreshFileScan() {
        viewModel.scanLargeFiles(viewModel.state.value.files.minBytes)
    }
}

private fun Intent.withActivityFlag(): Intent = apply {
    flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
}
