package com.kitsunetech.sweep.data.storage

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.kitsunetech.sweep.domain.DeletePlan
import com.kitsunetech.sweep.domain.FileSafetyPolicy
import com.kitsunetech.sweep.domain.FileSource
import com.kitsunetech.sweep.domain.StorageFile
import com.kitsunetech.sweep.domain.buildDeletePlan
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

data class DirectDeletionResult(
    val deletedIds: List<String>,
    val failedIds: List<String>,
)

data class DeletionRequest(
    val directPlan: DeletePlan,
    val mediaStoreIntentSender: IntentSender?,
    val unsupportedIds: List<String>,
)

fun deleteConfirmedDirectFiles(
    plan: DeletePlan,
    roots: Set<Path>,
    safetyPolicy: FileSafetyPolicy,
): DirectDeletionResult {
    val deleted = mutableListOf<String>()
    val failed = mutableListOf<String>()
    plan.files.forEach { file ->
        val path = file.safeDirectPath()
        if (path == null || !safetyPolicy.canDelete(path, roots)) {
            failed += file.id
            return@forEach
        }
        val wasDeleted = runCatching { Files.deleteIfExists(path) }.getOrDefault(false)
        if (wasDeleted) deleted += file.id else failed += file.id
    }
    return DirectDeletionResult(deletedIds = deleted, failedIds = failed)
}

class FileDeletionCoordinator(
    context: Context,
    private val roots: Set<Path>,
    private val safetyPolicy: FileSafetyPolicy = FileSafetyPolicy(),
) {
    private val resolver = context.applicationContext.contentResolver

    fun createRequest(plan: DeletePlan): DeletionRequest {
        val directPlan = buildDeletePlan(plan.files.filter { it.source == FileSource.DIRECT })
        val mediaFiles = plan.files.filter { it.source == FileSource.MEDIA_STORE }
        val validUris = mediaFiles.mapNotNull { file -> file.validContentUri() }
        val validUriIds = mediaFiles
            .filter { it.validContentUri() != null }
            .mapTo(hashSetOf()) { it.id }
        val unsupported = mediaFiles
            .filterNot { it.id in validUriIds }
            .map { it.id }
            .toMutableList()

        val sender = if (validUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(resolver, validUris).intentSender
        } else {
            if (validUris.isNotEmpty()) unsupported += validUriIds
            null
        }
        return DeletionRequest(
            directPlan = directPlan,
            mediaStoreIntentSender = sender,
            unsupportedIds = unsupported.distinct(),
        )
    }

    fun deleteConfirmedDirectFiles(plan: DeletePlan): DirectDeletionResult =
        deleteConfirmedDirectFiles(plan, roots, safetyPolicy)
}

private fun StorageFile.safeDirectPath(): Path? {
    if (source != FileSource.DIRECT || contentUri != null) return null
    val rawPath = path?.takeIf { it.isNotBlank() } ?: return null
    return try {
        Paths.get(rawPath)
    } catch (_: InvalidPathException) {
        null
    }
}

private fun StorageFile.validContentUri(): Uri? {
    if (source != FileSource.MEDIA_STORE || path != null) return null
    val parsed = contentUri?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return null
    return parsed.takeIf { it.scheme == "content" }
}
