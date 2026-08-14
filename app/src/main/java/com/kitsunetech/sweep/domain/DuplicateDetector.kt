package com.kitsunetech.sweep.domain

import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

fun interface ContentHasher {
    suspend fun sha256(file: StorageFile): String
}

fun interface ContentStreamOpener {
    fun open(file: StorageFile): InputStream
}

class Sha256ContentHasher(
    private val streamOpener: ContentStreamOpener,
) : ContentHasher {
    override suspend fun sha256(file: StorageFile): String {
        val context = currentCoroutineContext()
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        streamOpener.open(file).use { input ->
            while (true) {
                context.ensureActive()
                val bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}

data class HashProgress(
    val completed: Int,
    val total: Int,
)

data class DuplicateGroup(
    val sha256: String,
    val files: List<StorageFile>,
    val reclaimableBytes: Long,
)

class DuplicateDetector(
    private val hasher: ContentHasher,
) {
    suspend fun findExact(
        files: List<StorageFile>,
        onProgress: (HashProgress) -> Unit,
    ): List<DuplicateGroup> {
        val candidates = files
            .distinctBy { it.id }
            .filter { it.sizeBytes > 0L }
            .groupBy { it.sizeBytes }
            .values
            .filter { it.size > 1 }
            .flatten()
        val context = currentCoroutineContext()
        val hashes = linkedMapOf<String, MutableList<StorageFile>>()

        candidates.forEachIndexed { index, file ->
            context.ensureActive()
            val hash = hasher.sha256(file)
            hashes.getOrPut(hash) { mutableListOf() } += file
            onProgress(HashProgress(index + 1, candidates.size))
        }

        return hashes
            .asSequence()
            .filter { (_, groupFiles) -> groupFiles.size > 1 }
            .map { (hash, groupFiles) ->
                val sortedFiles = groupFiles.sortedBy { it.path ?: it.contentUri ?: it.displayName }
                DuplicateGroup(
                    sha256 = hash,
                    files = sortedFiles,
                    reclaimableBytes = sortedFiles
                        .drop(1)
                        .sumOf { it.sizeBytes.coerceAtLeast(0L) },
                )
            }
            .sortedByDescending { it.reclaimableBytes }
            .toList()
    }
}
