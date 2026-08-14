package com.kitsunetech.sweep.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kitsunetech.sweep.domain.AppRecord
import com.kitsunetech.sweep.domain.toReadableBytes
import com.kitsunetech.sweep.ui.AppSort
import com.kitsunetech.sweep.ui.AppsState
import com.kitsunetech.sweep.ui.components.ScanStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun AppsScreen(
    state: AppsState,
    onLoad: () -> Unit,
    onSort: (AppSort) -> Unit,
    onAppDetails: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onClearCaches: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Review apps", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Sort by real storage use or the last date Android recorded.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SortChip("Total size", AppSort.TOTAL_SIZE, state.sort, onSort)
            SortChip("Cache", AppSort.CACHE, state.sort, onSort)
            SortChip("Oldest use", AppSort.OLDEST_USE, state.sort, onSort)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onLoad, modifier = Modifier.weight(1f)) { Text("Refresh apps") }
            OutlinedButton(onClick = onClearCaches, modifier = Modifier.weight(1f)) {
                Text("Clear cache in Android")
            }
        }
        ScanStatus(
            isLoading = state.isLoading,
            progressText = state.progress?.let { "${it.completed} of ${it.total} apps checked" },
            error = state.error,
        )
        if (!state.isLoading && state.apps.isEmpty()) Text("No apps loaded yet")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.apps, key = AppRecord::packageName) { app ->
                AppCard(app, onAppDetails, onUninstall)
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    value: AppSort,
    selected: AppSort,
    onSort: (AppSort) -> Unit,
) {
    FilterChip(selected = value == selected, onClick = { onSort(value) }, label = { Text(label) })
}

@Composable
private fun AppCard(
    app: AppRecord,
    onAppDetails: (String) -> Unit,
    onUninstall: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(app.totalBytes?.toReadableBytes() ?: "Size unavailable")
            }
            Text("Cache ${app.cacheBytes?.toReadableBytes() ?: "unavailable"}")
            Text(lastUsedText(app.lastUsedAtMillis))
            if (app.isCold) Text("Not used in at least 90 days", color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onAppDetails(app.packageName) }) {
                    Text("Android settings")
                }
                if (!app.isSystem) {
                    OutlinedButton(onClick = { onUninstall(app.packageName) }) {
                        Text("Uninstall")
                    }
                }
            }
        }
    }
}

private fun lastUsedText(millis: Long?): String = if (millis != null && millis > 0L) {
    "Last used ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))}"
} else {
    "Usage unknown"
}
