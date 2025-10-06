package com.github.pinball83.maskededittext

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskFormatterTest {

    private val phoneFormatter = MaskFormatter(
        maskPattern = "8 (***) *** **-**",
        placeholder = '*',
        formatPattern = "[1][2][3] [4][5][6]-[7][8]-[10][9]"
    )

    @Test
    fun `mask applies template to numeric input`() {
        val masked = phoneFormatter.mask("1234567890")
        assertEquals("8 (123) 456 78-90", masked)
    }

    @Test
    fun `unmask preserves slot padding`() {
        val partialMasked = "8 (123) 45  --  "
        val unmasked = phoneFormatter.unmask(partialMasked)
        assertEquals("12345     ", unmasked)
    }

    @Test
    fun `format output follows substitution order`() {
        val formatted = phoneFormatter.formatOutput("1234567890")
        assertEquals("123 456-78-09", formatted)
    }

    @Test
    fun `is complete requires every slot`() {
        val incomplete = phoneFormatter.isComplete("12345")
        val complete = phoneFormatter.isComplete("1234567890")

        assertEquals(false, incomplete)
        assertEquals(true, complete)
    }
}
