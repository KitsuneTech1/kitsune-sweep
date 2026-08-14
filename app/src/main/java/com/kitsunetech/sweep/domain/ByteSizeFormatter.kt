package com.kitsunetech.sweep.domain

import java.util.Locale
import kotlin.math.floor

private val byteUnits = listOf("B", "KB", "MB", "GB", "TB")

fun Long.toReadableBytes(): String {
    val safeBytes = coerceAtLeast(0L)
    if (safeBytes < 1024L) return "$safeBytes B"

    var value = safeBytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < byteUnits.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }

    val format = if (value < 10.0 && value != floor(value)) "%.1f %s" else "%.0f %s"
    return String.format(Locale.US, format, value, byteUnits[unitIndex])
}
