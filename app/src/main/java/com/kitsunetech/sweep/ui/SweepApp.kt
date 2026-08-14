package com.kitsunetech.sweep.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.kitsunetech.sweep.domain.DeletePlan
import com.kitsunetech.sweep.domain.toReadableBytes
import com.kitsunetech.sweep.ui.screens.AppsScreen
import com.kitsunetech.sweep.ui.screens.DuplicatesScreen
import com.kitsunetech.sweep.ui.screens.FilesScreen
import com.kitsunetech.sweep.ui.screens.HomeScreen

data class SweepActions(
    val onNavigate: (SweepDestination) -> Unit = {},
    val onRequestAllFiles: () -> Unit = {},
    val onRequestUsage: () -> Unit = {},
    val onOpenStorageTools: () -> Unit = {},
    val onClearCaches: () -> Unit = {},
    val onScanLargeFiles: (Long) -> Unit = {},
    val onToggleFile: (String) -> Unit = {},
    val onScanDuplicates: () -> Unit = {},
    val onToggleDuplicateFile: (String) -> Unit = {},
    val onLoadApps: () -> Unit = {},
    val onSortApps: (AppSort) -> Unit = {},
    val onAppDetails: (String) -> Unit = {},
    val onUninstall: (String) -> Unit = {},
    val onReviewDeletion: (DeletePlan) -> Unit = {},
    val onConfirmDeletion: (DeletePlan) -> Unit = {},
)

@Composable
fun SweepApp(
    state: SweepUiState,
    actions: SweepActions,
    modifier: Modifier = Modifier,
) {
    var planUnderReview by remember { mutableStateOf<DeletePlan?>(null) }
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val review: (DeletePlan) -> Unit = { plan ->
        actions.onReviewDeletion(plan)
        planUnderReview = plan
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                if (largeText) {
                    Column {
                        NAV_ITEMS.chunked(2).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth()) {
                                rowItems.forEach { item ->
                                    SweepNavigationItem(
                                        item = item,
                                        selected = state.destination == item.destination,
                                        onNavigate = actions.onNavigate,
                                        labelStyle = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    NAV_ITEMS.forEach { item ->
                        SweepNavigationItem(
                            item = item,
                            selected = state.destination == item.destination,
                            onNavigate = actions.onNavigate,
                            labelStyle = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (state.destination) {
            SweepDestination.HOME -> HomeScreen(
                state = state.home,
                onRequestAllFiles = actions.onRequestAllFiles,
                onRequestUsage = actions.onRequestUsage,
                onOpenStorageTools = actions.onOpenStorageTools,
                onClearCaches = actions.onClearCaches,
                modifier = Modifier.padding(padding),
            )
            SweepDestination.FILES -> FilesScreen(
                state = state.files,
                onScan = actions.onScanLargeFiles,
                onToggleFile = actions.onToggleFile,
                onReviewDelete = review,
                modifier = Modifier.padding(padding),
            )
            SweepDestination.DUPLICATES -> DuplicatesScreen(
                state = state.duplicates,
                onScan = actions.onScanDuplicates,
                onToggleFile = actions.onToggleDuplicateFile,
                onReviewDelete = review,
                modifier = Modifier.padding(padding),
            )
            SweepDestination.APPS -> AppsScreen(
                state = state.apps,
                onLoad = actions.onLoadApps,
                onSort = actions.onSortApps,
                onAppDetails = actions.onAppDetails,
                onUninstall = actions.onUninstall,
                onClearCaches = actions.onClearCaches,
                modifier = Modifier.padding(padding),
            )
        }
    }

    planUnderReview?.let { plan ->
        AlertDialog(
            onDismissRequest = { planUnderReview = null },
            title = {
                Text("Delete ${plan.files.size} files totaling ${plan.totalBytes.toReadableBytes()}?")
            },
            text = { Text("Only these files will be requested for deletion.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        planUnderReview = null
                        actions.onConfirmDeletion(plan)
                    },
                ) {
                    Text("Delete files")
                }
            },
            dismissButton = {
                TextButton(onClick = { planUnderReview = null }) { Text("Cancel") }
            },
        )
    }
}

private data class NavigationItem(
    val destination: SweepDestination,
    val marker: String,
    val label: String,
)

@Composable
private fun RowScope.SweepNavigationItem(
    item: NavigationItem,
    selected: Boolean,
    onNavigate: (SweepDestination) -> Unit,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    NavigationBarItem(
        selected = selected,
        onClick = { onNavigate(item.destination) },
        icon = {
            Text(
                item.marker,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = "${item.label} section"
                },
            )
        },
        label = {
            Text(
                item.label,
                maxLines = 1,
                softWrap = false,
                style = labelStyle,
                modifier = Modifier.testTag("navigation-${item.destination.name.lowercase()}"),
            )
        },
        alwaysShowLabel = true,
        modifier = modifier,
    )
}

private val NAV_ITEMS = listOf(
    NavigationItem(SweepDestination.HOME, "H", "Home"),
    NavigationItem(SweepDestination.FILES, "F", "Files"),
    NavigationItem(SweepDestination.DUPLICATES, "D", "Duplicates"),
    NavigationItem(SweepDestination.APPS, "A", "Apps"),
)
