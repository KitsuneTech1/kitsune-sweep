package com.kitsunetech.sweep.domain

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

class FileSafetyPolicy(
    private val protectedTopLevelNames: Set<String> = setOf("android"),
) {
    fun canDelete(path: Path, roots: Set<Path>): Boolean {
        val candidate = path.toAbsolutePath().normalize()
        val root = roots
            .asSequence()
            .map { it.toAbsolutePath().normalize() }
            .filter { candidate.startsWith(it) }
            .maxByOrNull { it.nameCount }
            ?: return false

        val relative = root.relativize(candidate)
        if (relative.nameCount == 0) return false
        if (relative.first().toString().lowercase() in protectedTopLevelNames) return false

        return runCatching {
            !Files.isSymbolicLink(candidate) &&
                Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)
        }.getOrDefault(false)
    }
}

