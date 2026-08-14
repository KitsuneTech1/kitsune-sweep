package com.kitsunetech.sweep.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColdAppPolicyTest {
    private val now = 1_800_000_000_000L

    @Test
    fun marksUserAppColdAfterNinetyDays() {
        assertTrue(classifyColdApp(false, now - 91L * DAY, now))
    }

    @Test
    fun keepsRecentlyUsedAppWarm() {
        assertFalse(classifyColdApp(false, now - 89L * DAY, now))
    }

    @Test
    fun neverMarksSystemAppCold() {
        assertFalse(classifyColdApp(true, now - 200L * DAY, now))
    }

    @Test
    fun treatsMissingInvalidAndFutureTimestampsAsUnknown() {
        assertFalse(classifyColdApp(false, null, now))
        assertFalse(classifyColdApp(false, 0L, now))
        assertFalse(classifyColdApp(false, now + DAY, now))
    }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
    }
}
