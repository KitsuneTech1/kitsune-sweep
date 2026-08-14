package com.kitsunetech.sweep.data.storage

import com.kitsunetech.sweep.domain.FileSource
import com.kitsunetech.sweep.domain.StorageFile
import com.kitsunetech.sweep.domain.buildDeletePlan
import org.junit.Assert.assertEquals
import org.junit.Test

class DeletionOutcomeTest {
    @Test
    fun classifiesEveryRequestedMediaUriAfterApproval() {
        val removed = media("removed")
        val remaining = media("remaining")
        val queried = mutableListOf<String>()

        val result = classifyMediaStoreDeletion(buildDeletePlan(listOf(removed, remaining))) { file ->
            queried += file.id
            file.id == "remaining"
        }

        assertEquals(listOf("removed", "remaining"), queried)
        assertEquals(listOf("removed"), result.deletedIds)
        assertEquals(listOf("remaining"), result.failedIds)
    }

    @Test
    fun combinesMediaDirectAndUnsupportedResultsWithRemainingLocations() {
        val directDeleted = direct("direct-deleted")
        val directRemaining = direct("direct-remaining")
        val mediaDeleted = media("media-deleted")
        val mediaRemaining = media("media-remaining")
        val unsupported = media("unsupported")
        val request = DeletionRequest(
            directPlan = buildDeletePlan(listOf(directDeleted, directRemaining)),
            mediaStorePlan = buildDeletePlan(listOf(mediaDeleted, mediaRemaining)),
            mediaStoreIntentSender = null,
            unsupportedFiles = listOf(unsupported),
        )

        val outcome = combineDeletionResults(
            request = request,
            directResult = DirectDeletionResult(listOf("direct-deleted"), listOf("direct-remaining")),
            mediaResult = DirectDeletionResult(listOf("media-deleted"), listOf("media-remaining")),
        )

        assertEquals(2, outcome.deletedCount)
        assertEquals(
            listOf(
                "/storage/direct-remaining.bin",
                "content://media/media-remaining",
                "content://media/unsupported",
            ),
            outcome.remainingLocations,
        )
    }

    private fun direct(id: String) = StorageFile(
        id = id,
        displayName = "$id.bin",
        path = "/storage/$id.bin",
        contentUri = null,
        sizeBytes = 10L,
        modifiedAtMillis = 20L,
        mimeType = null,
        source = FileSource.DIRECT,
    )

    private fun media(id: String) = StorageFile(
        id = id,
        displayName = "$id.bin",
        path = null,
        contentUri = "content://media/$id",
        sizeBytes = 10L,
        modifiedAtMillis = 20L,
        mimeType = null,
        source = FileSource.MEDIA_STORE,
    )
}
