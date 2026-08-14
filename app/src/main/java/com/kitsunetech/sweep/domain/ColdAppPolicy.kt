package com.kitsunetech.sweep.domain

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

fun classifyColdApp(
    isSystem: Boolean,
    lastUsedAtMillis: Long?,
    nowMillis: Long,
    thresholdDays: Long = 90L,
): Boolean {
    if (isSystem || lastUsedAtMillis == null || lastUsedAtMillis <= 0L) return false
    if (lastUsedAtMillis > nowMillis || thresholdDays <= 0L) return false
    return nowMillis - lastUsedAtMillis >= thresholdDays * MILLIS_PER_DAY
}
