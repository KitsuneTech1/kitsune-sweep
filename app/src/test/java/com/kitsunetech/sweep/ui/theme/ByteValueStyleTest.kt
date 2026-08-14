package com.kitsunetech.sweep.ui.theme

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ByteValueStyleTest {
    @Test
    fun usesADedicatedCondensedDeviceFamilyInsteadOfGenericSans() {
        assertNotEquals(FontFamily.SansSerif, ByteValueStyle.fontFamily)
    }
}
