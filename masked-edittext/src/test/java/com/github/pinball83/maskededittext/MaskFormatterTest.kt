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
        val partialMasked = phoneFormatter.mask("12345")
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

    @Test
    fun `cursorPositionFor moves to next slot after last filled`() {
        assertEquals(phoneFormatter.firstValidPosition(), phoneFormatter.cursorPositionFor(0))
        val positions = phoneFormatter.validPositions
        for (i in 1..positions.size) {
            val expected = if (i >= positions.size) positions.last() else positions[i]
            assertEquals(expected, phoneFormatter.cursorPositionFor(i))
        }
    }

    @Test
    fun `nearestValidPosition prefers forward slot on tie`() {
        val positions = phoneFormatter.validPositions
        for (idx in 0 until positions.size - 1) {
            val a = positions[idx]
            val b = positions[idx + 1]
            val mid = (a + b) / 2
            val chosen = phoneFormatter.nearestValidPosition(mid)
            val distUp = b - mid
            val distDown = mid - a
            val expected = if (distUp <= distDown) b else a
            assertEquals(expected, chosen)
        }
    }

    @Test
    fun `deleting last digit repositions cursor at previous slot`() {
        val reMasked = phoneFormatter.mask("123456789")
        assertEquals("8 (123) 456 78-9 ", reMasked)
        val expectedCursor = phoneFormatter.cursorPositionFor(9)
        val lastPos = phoneFormatter.validPositions[9]
        assertEquals(lastPos, expectedCursor)
    }
}
