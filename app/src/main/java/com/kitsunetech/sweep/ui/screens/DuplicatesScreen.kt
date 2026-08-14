package com.kitsunetech.sweep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kitsunetech.sweep.domain.DeletePlan
import com.kitsunetech.sweep.domain.DuplicateGroup
import com.kitsunetech.sweep.domain.StorageFile
import com.kitsunetech.sweep.domain.buildDeletePlan
import com.kitsunetech.sweep.domain.toReadableBytes
import com.kitsunetech.sweep.ui.DuplicatesState
import com.kitsunetech.sweep.ui.components.ScanStatus
import com.kitsunetech.sweep.ui.theme.PathStyle
import com.kitsunetech.sweep.ui.theme.ByteValueStyle

@Composable
fun DuplicatesScreen(
    state: DuplicatesState,
    onScan: () -> Unit,
    onToggleFile: (String) -> Unit,
    onReviewDelete: (DeletePlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    val files = state.groups.flatMap(DuplicateGroup::files)
    val selectedPlan = buildDeletePlan(files.filter { it.id in state.selectedIds })
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Exact duplicates", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Matches require the same size and SHA-256 hash. Pick every file yourself.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
            Text("Find exact duplicates")
        }
        ScanStatus(
            isLoading = state.isLoading,
            progressText = state.progress?.let { "${it.completed} of ${it.total} files hashed" },
            error = state.error,
        )
        if (!state.isLoading && state.groups.isEmpty()) {
            Text("No exact duplicates found")
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.groups, key = DuplicateGroup::sha256) { group ->
                DuplicateGroupCard(group, state.selectedIds, onToggleFile)
            }
        }
        Text(
            if (selectedPlan.files.isEmpty()) {
                "No duplicates selected"
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
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedIds: Set<String>,
    onToggleFile: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${group.files.size} copies, up to", style = MaterialTheme.typography.titleSmall)
                Text(group.reclaimableBytes.toReadableBytes(), style = ByteValueStyle)
                Text("reclaimable", style = MaterialTheme.typography.titleSmall)
            }
            group.files.forEach { file ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = file.id in selectedIds,
                        onCheckedChange = { onToggleFile(file.id) },
                        modifier = Modifier.testTag("duplicate-${file.id}"),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.displayName)
                        Text(
                            file.path ?: file.contentUri ?: "Location unavailable",
                            style = PathStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(file.sizeBytes.toReadableBytes(), style = ByteValueStyle)
                }
            }
        }
    }
}
