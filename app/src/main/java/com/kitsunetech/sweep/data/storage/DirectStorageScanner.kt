package com.kitsunetech.sweep.data.storage

import com.kitsunetech.sweep.domain.FileSource
import com.kitsunetech.sweep.domain.StorageFile
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class DirectStorageScanner(
    roots: Set<Path>,
) : StorageScanner {
    private val normalizedRoots = roots.mapTo(linkedSetOf()) { it.toAbsolutePath().normalize() }

    override suspend fun scanLargeFiles(
        minBytes: Long,
        onProgress: (ScanProgress) -> Unit,
    ): ScanResult {
        val safeMinimum = minBytes.coerceAtLeast(0L)
        val matches = mutableListOf<StorageFile>()
        val errors = mutableListOf<String>()
        val context = currentCoroutineContext()
        var visited = 0L
        var skipped = 0

        normalizedRoots.forEach { root ->
            context.ensureActive()
            if (!Files.isDirectory(root)) {
                errors += "Storage root is unavailable: $root"
                return@forEach
            }

            Files.walkFileTree(
                root,
                visitor(
                    root = root,
                    minBytes = safeMinimum,
                    context = context,
                    matches = matches,
                    errors = errors,
                    onVisit = {
                        visited += 1
                        if (visited % PROGRESS_INTERVAL == 0L) {
                            onProgress(ScanProgress(visited, matches.size))
                        }
                    },
                    onSkip = { skipped += 1 },
                ),
            )
        }

        onProgress(ScanProgress(visited, matches.size))
        return ScanResult(
            files = matches.sortedByDescending { it.sizeBytes },
            skipped = skipped,
            errors = errors,
        )
    }

    private fun visitor(
        root: Path,
        minBytes: Long,
        context: CoroutineContext,
        matches: MutableList<StorageFile>,
        errors: MutableList<String>,
        onVisit: () -> Unit,
        onSkip: () -> Unit,
    ) = object : FileVisitor<Path> {
        override fun preVisitDirectory(
            directory: Path,
            attributes: BasicFileAttributes,
        ): FileVisitResult {
            context.ensureActive()
            if (directory != root) {
                val relative = root.relativize(directory)
                if (relative.nameCount > 0 && relative.first().toString().equals("Android", true)) {
                    onSkip()
                    return FileVisitResult.SKIP_SUBTREE
                }
            }
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(
            file: Path,
            attributes: BasicFileAttributes,
        ): FileVisitResult {
            context.ensureActive()
            onVisit()
            if (attributes.isRegularFile && attributes.size() >= minBytes) {
                val path = file.toAbsolutePath().normalize()
                matches += StorageFile(
                    id = path.toString(),
                    displayName = path.fileName?.toString().orEmpty(),
                    path = path.toString(),
                    contentUri = null,
                    sizeBytes = attributes.size(),
                    modifiedAtMillis = attributes.lastModifiedTime().toMillis(),
                    mimeType = runCatching { Files.probeContentType(path) }.getOrNull(),
                    source = FileSource.DIRECT,
                )
            }
            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(file: Path, error: IOException): FileVisitResult {
            context.ensureActive()
            onSkip()
            errors += "Could not read: $file"
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(directory: Path, error: IOException?): FileVisitResult {
            context.ensureActive()
            if (error != null) {
                onSkip()
                errors += "Could not finish reading: $directory"
            }
            return FileVisitResult.CONTINUE
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL = 128L
    }
}
