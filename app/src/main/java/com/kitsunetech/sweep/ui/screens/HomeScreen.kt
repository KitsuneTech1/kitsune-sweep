package com.kitsunetech.sweep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kitsunetech.sweep.ui.HomeState
import com.kitsunetech.sweep.ui.components.PermissionCard
import com.kitsunetech.sweep.ui.components.StorageStrata

@Composable
fun HomeScreen(
    state: HomeState,
    onRequestUsage: () -> Unit,
    onOpenStorageTools: () -> Unit,
    onClearCaches: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Kitsune Sweep", style = MaterialTheme.typography.displaySmall)
            Text(
                "Storage, without the scare tactics.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            StorageStrata(
                totalBytes = state.totalBytes,
                freeBytes = state.freeBytes,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        item {
            PermissionCard(
                title = "Usage Access",
                explanation = "Lets app review read storage size and last-used dates.",
                granted = state.permissions.usageAccess,
                onRequest = onRequestUsage,
            )
        }
        item {
            Button(onClick = onOpenStorageTools, modifier = Modifier.fillMaxWidth()) {
                Text("Samsung storage tools")
            }
        }
        item {
            OutlinedButton(onClick = onClearCaches, modifier = Modifier.fillMaxWidth()) {
                Text("Clear cache in Android")
            }
            Text(
                "Android shows what it can clear and asks before it acts. Cache usually helps apps load faster.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
