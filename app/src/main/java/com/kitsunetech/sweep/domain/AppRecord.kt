package com.kitsunetech.sweep.domain

data class AppRecord(
    val packageName: String,
    val label: String,
    val appBytes: Long?,
    val dataBytes: Long?,
    val cacheBytes: Long?,
    val lastUsedAtMillis: Long?,
    val firstInstalledAtMillis: Long,
    val isSystem: Boolean,
    val isCold: Boolean,
) {
    val totalBytes: Long?
        get() {
            val sizes = listOfNotNull(appBytes, dataBytes)
            return if (sizes.isEmpty()) null else sizes.sumOf { it.coerceAtLeast(0L) }
        }
}
