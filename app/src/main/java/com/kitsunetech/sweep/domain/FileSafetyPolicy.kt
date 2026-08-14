package com.kitsunetech.sweep.domain

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

data class ApprovedDeleteTarget(
    val root: Path,
    val relativePath: Path,
)

class FileSafetyPolicy(
    private val protectedTopLevelNames: Set<String> = setOf("android"),
    private val protectedRoots: Set<Path> = emptySet(),
) {
    fun canDelete(path: Path, roots: Set<Path>): Boolean {
        return approvedTarget(path, roots) != null
    }

    fun approvedTarget(path: Path, roots: Set<Path>): ApprovedDeleteTarget? {
        val candidate = path.toAbsolutePath().normalize()
        val root = roots
            .asSequence()
            .map { it.toAbsolutePath().normalize() }
            .filterNot(::isProtectedRoot)
            .filter { candidate.startsWith(it) }
            .maxByOrNull { it.nameCount }
            ?: return null

        val relative = root.relativize(candidate)
        if (relative.nameCount == 0) return null
        if (relative.first().toString().lowercase() in protectedTopLevelNames) return null
        if (!hasOnlyNoFollowComponents(root, relative)) return null

        return ApprovedDeleteTarget(root = root, relativePath = relative)
    }

    private fun isProtectedRoot(root: Path): Boolean {
        if (protectedRoots.any { root.startsWith(it.toAbsolutePath().normalize()) }) return true
        return root.any { it.toString().lowercase() in protectedTopLevelNames }
    }

    private fun hasOnlyNoFollowComponents(root: Path, relative: Path): Boolean {
        val filesystemRoot = root.root ?: return false
        val components = filesystemRoot.relativize(root).toList() + relative.toList()
        if (components.isEmpty()) return false

        var current = filesystemRoot
        return components.withIndex().all { (index, component) ->
            current = current.resolve(component)
            val attributes = runCatching {
                Files.readAttributes(current, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            }.getOrNull() ?: return false
            if (index == components.lastIndex) {
                attributes.isRegularFile
            } else {
                attributes.isDirectory && !attributes.isSymbolicLink
            }
        }
    }
}
