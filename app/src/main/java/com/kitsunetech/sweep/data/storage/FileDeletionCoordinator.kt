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
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SecureDirectoryStream
import java.nio.file.attribute.BasicFileAttributeView

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
        val target = path?.let { safetyPolicy.approvedTarget(it, roots) }
        if (target == null) {
            failed += file.id
            return@forEach
        }
        val wasDeleted = deleteAnchoredRegularFile(target.root, target.relativePath)
        if (wasDeleted) deleted += file.id else failed += file.id
    }
    return DirectDeletionResult(deletedIds = deleted, failedIds = failed)
}

private fun deleteAnchoredRegularFile(root: Path, relativePath: Path): Boolean {
    if (relativePath.nameCount == 0) return false
    val rootDirectory = openSecureDirectory(root) ?: return false
    return rootDirectory.use { deleteRelativeRegularFile(it, relativePath.toList()) }
}

internal fun supportsAnchoredDirectDeletion(root: Path): Boolean {
    val directory = openSecureDirectory(root) ?: return false
    directory.close()
    return true
}

private fun openSecureDirectory(directory: Path): SecureDirectoryStream<Path>? {
    val absoluteDirectory = directory.toAbsolutePath().normalize()
    return runCatching { Files.newDirectoryStream(absoluteDirectory) }.getOrNull()
        as? SecureDirectoryStream<Path>
}

private fun deleteRelativeRegularFile(
    directory: SecureDirectoryStream<Path>,
    components: List<Path>,
): Boolean {
    val next = components.firstOrNull() ?: return false
    if (components.size > 1) {
        return runCatching {
            directory.newDirectoryStream(next, LinkOption.NOFOLLOW_LINKS).use { child ->
                deleteRelativeRegularFile(child, components.drop(1))
            }
        }.getOrDefault(false)
    }

    return runCatching {
        val attributes = directory
            .getFileAttributeView(next, BasicFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS)
            ?.readAttributes()
            ?: return false
        if (!attributes.isRegularFile) return false
        directory.deleteFile(next)
        true
    }.getOrDefault(false)
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
