package com.kitsunetech.sweep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsunetech.sweep.domain.DeletePlan
import com.kitsunetech.sweep.domain.StorageFile
import com.kitsunetech.sweep.domain.buildDeletePlan
import com.kitsunetech.sweep.domain.toReadableBytes
import com.kitsunetech.sweep.ui.FilesState
import com.kitsunetech.sweep.ui.components.ScanStatus
import com.kitsunetech.sweep.ui.theme.PathStyle
import java.text.DateFormat
import java.util.Date

@Composable
fun FilesScreen(
    state: FilesState,
    onScan: (Long) -> Unit,
    onToggleFile: (String) -> Unit,
    onReviewDelete: (DeletePlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedPlan = buildDeletePlan(state.files.filter { it.id in state.selectedIds })
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Large files", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Review shared files by size. Nothing is selected automatically.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FILE_THRESHOLDS.forEach { threshold ->
                FilterChip(
                    selected = state.minBytes == threshold,
                    onClick = { onScan(threshold) },
                    label = { Text(threshold.toReadableBytes()) },
                    modifier = Modifier.testTag("threshold-${threshold.toReadableBytes()}"),
                )
            }
        }
        Button(onClick = { onScan(state.minBytes) }, modifier = Modifier.fillMaxWidth()) {
            Text("Scan large files")
        }
        ScanStatus(
            isLoading = state.isLoading,
            progressText = state.progress?.let { "${it.visited} files checked, ${it.matched} matched" },
            error = state.error,
        )
        if (!state.isLoading && state.files.isEmpty()) {
            Text("Nothing over ${state.minBytes.toReadableBytes()}")
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.files, key = StorageFile::id) { file ->
                FileRow(
                    file = file,
                    selected = file.id in state.selectedIds,
                    onToggle = { onToggleFile(file.id) },
                )
            }
        }
        Text(
            if (selectedPlan.files.isEmpty()) {
                "No files selected"
            } else {
                "${selectedPlan.files.size} selected, ${selectedPlan.totalBytes.toReadableBytes()}"
            },
            style = MaterialTheme.typography.labelLarge,
        )
        OutlinedButton(
            onClick = { onReviewDelete(selectedPlan) },
            enabled = selectedPlan.files.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Review deletion")
        }
    }
}

@Composable
private fun FileRow(
    file: StorageFile,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("select-${file.id}"),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(file.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    file.path ?: file.contentUri ?: "Location unavailable",
                    style = PathStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    modifiedText(file.modifiedAtMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(file.sizeBytes.toReadableBytes(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun modifiedText(millis: Long): String = if (millis > 0L) {
    "Modified ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))}"
} else {
    "Modified date unknown"
}

private val FILE_THRESHOLDS = listOf(
    50L * 1024L * 1024L,
    100L * 1024L * 1024L,
    250L * 1024L * 1024L,
    500L * 1024L * 1024L,
    1024L * 1024L * 1024L,
)
