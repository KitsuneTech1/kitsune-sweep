package com.kitsunetech.sweep.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.kitsunetech.sweep.domain.AppRecord
import com.kitsunetech.sweep.domain.FileSource
import com.kitsunetech.sweep.domain.StorageFile
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SweepAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exposesHonestDashboardAndEmptyStates() {
        composeRule.setContent {
            var state by remember { mutableStateOf(sampleState()) }
            SweepApp(
                state = state,
                actions = statefulActions(state) { state = it },
            )
        }

        composeRule.onNodeWithText("Kitsune Sweep").assertIsDisplayed()
        composeRule.onNodeWithText("Storage, without the scare tactics.").assertIsDisplayed()
        composeRule.onNodeWithText("Samsung storage tools").assertIsDisplayed()
        composeRule.onNodeWithText("All Files Access").assertIsDisplayed()
        composeRule.onNodeWithText("Usage Access").assertIsDisplayed()

        composeRule.onNodeWithText("Files").performClick()
        composeRule.onNodeWithText("Scan large files").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing over 100 MB").assertIsDisplayed()
        composeRule.onNodeWithText("Review deletion").assertIsNotEnabled()

        composeRule.onNodeWithText("Duplicates").performClick()
        composeRule.onNodeWithText("Find exact duplicates").assertIsDisplayed()
        composeRule.onNodeWithText("No exact duplicates found").assertIsDisplayed()

        composeRule.onNodeWithText("Apps").performClick()
        composeRule.onNodeWithText("Review apps").assertIsDisplayed()
        composeRule.onNodeWithText("Usage unknown").assertIsDisplayed()
        composeRule.onNodeWithText("Clear cache in Android").assertIsDisplayed()
    }

    @Test
    fun reviewsExactSelectionBeforeDeletion() {
        composeRule.setContent {
            var state by remember { mutableStateOf(sampleState(withFiles = true)) }
            SweepApp(
                state = state.copy(destination = SweepDestination.FILES),
                actions = statefulActions(state) { state = it },
            )
        }

        composeRule.onNodeWithTag("select-a").performClick()
        composeRule.onNodeWithTag("select-b").performClick()
        composeRule.onNodeWithText("2 selected, 3 MB").assertIsDisplayed()
        composeRule.onNodeWithText("Review deletion").performClick()
        composeRule.onNodeWithText("Delete 2 files totaling 3 MB?").assertIsDisplayed()
        composeRule.onNodeWithText("Only these files will be requested for deletion.").assertIsDisplayed()
        composeRule.onNodeWithText("Delete files").assertIsDisplayed()
    }

    @Test
    fun keepsNavigationAndPrimaryActionVisibleAtLargeText() {
        composeRule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(current.density, fontScale = 2f),
            ) {
                var state by remember { mutableStateOf(sampleState()) }
                SweepApp(
                    state = state,
                    actions = statefulActions(state) { state = it },
                )
            }
        }

        composeRule.onNodeWithText("Files").performClick()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Files").assertIsDisplayed()
        composeRule.onNodeWithText("Duplicates").assertIsDisplayed()
        composeRule.onNodeWithText("Apps").assertIsDisplayed()
        composeRule.onNodeWithText("Scan large files").assertIsDisplayed()
    }

    @Test
    fun keepsAllFileThresholdsAndDuplicatesLabelInsideTheViewport() {
        composeRule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(current.density, fontScale = 2f),
            ) {
                var state by remember { mutableStateOf(sampleState()) }
                SweepApp(
                    state = state.copy(destination = SweepDestination.FILES),
                    actions = statefulActions(state) { state = it },
                )
            }
        }

        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        listOf("50 MB", "100 MB", "250 MB", "500 MB", "1 GB").forEach { label ->
            val chipBounds = composeRule.onNodeWithTag("threshold-$label").fetchSemanticsNode().boundsInRoot
            val labelBounds = composeRule.onNodeWithTag(
                "threshold-label-$label",
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot
            assertTrue("$label threshold must fit inside the viewport", chipBounds.right <= rootBounds.right)
            assertTrue("$label text must fit inside its chip", labelBounds.left >= chipBounds.left && labelBounds.right <= chipBounds.right)
        }

        val duplicatesBounds = composeRule.onNodeWithTag(
            "navigation-duplicates",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val maximumLabelHeight = with(composeRule.density) { 28.dp.toPx() }
        val minimumLabelHeight = with(composeRule.density) { 24.dp.toPx() }
        assertTrue("Duplicates label must stay on one line", duplicatesBounds.height <= maximumLabelHeight)
        assertTrue("Duplicates label must respect 200 percent text scaling", duplicatesBounds.height >= minimumLabelHeight)
    }

    private fun statefulActions(
        current: SweepUiState,
        update: (SweepUiState) -> Unit,
    ) = SweepActions(
        onNavigate = { update(current.copy(destination = it)) },
        onToggleFile = { id ->
            val selected = current.files.selectedIds.toMutableSet()
            if (!selected.add(id)) selected.remove(id)
            update(current.copy(files = current.files.copy(selectedIds = selected)))
        },
    )

    private fun sampleState(withFiles: Boolean = false): SweepUiState {
        val files = if (withFiles) {
            listOf(file("a", 1024L * 1024L), file("b", 2L * 1024L * 1024L))
        } else {
            emptyList()
        }
        return SweepUiState(
            home = HomeState(totalBytes = 256L * 1024L * 1024L * 1024L, freeBytes = 96L * 1024L * 1024L * 1024L),
            files = FilesState(files = files),
            apps = AppsState(
                apps = listOf(
                    AppRecord(
                        packageName = "com.example.reader",
                        label = "Reader",
                        appBytes = 20L * 1024L * 1024L,
                        dataBytes = 10L * 1024L * 1024L,
                        cacheBytes = 5L * 1024L * 1024L,
                        lastUsedAtMillis = null,
                        firstInstalledAtMillis = 0L,
                        isSystem = false,
                        isCold = false,
                    ),
                ),
            ),
        )
    }

    private fun file(id: String, size: Long) = StorageFile(
        id = id,
        displayName = "$id.bin",
        path = "/storage/emulated/0/Download/$id.bin",
        contentUri = null,
        sizeBytes = size,
        modifiedAtMillis = 0L,
        mimeType = "application/octet-stream",
        source = FileSource.DIRECT,
    )
}
