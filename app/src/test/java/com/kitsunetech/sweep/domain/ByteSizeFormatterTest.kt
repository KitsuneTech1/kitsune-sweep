package com.kitsunetech.sweep.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteSizeFormatterTest {
    @Test
    fun formatsZero() {
        assertEquals("0 B", 0L.toReadableBytes())
    }

    @Test
    fun formatsBinaryMegabytes() {
        assertEquals("1.5 MB", 1_572_864L.toReadableBytes())
    }

    @Test
    fun clampsNegativeValues() {
        assertEquals("0 B", (-20L).toReadableBytes())
    }
}
