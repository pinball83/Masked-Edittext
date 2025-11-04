package com.github.pinball83.maskededittext.compose

import com.github.pinball83.maskededittext.InputEvent
import com.github.pinball83.maskededittext.InputState
import com.github.pinball83.maskededittext.MaskFormatter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MaskedTextFieldBehaviorComposeTest {

    private lateinit var formatter: MaskFormatter
    private lateinit var state: MaskedTextFieldState

    @Before
    fun setUp() {
        formatter = MaskFormatter(
            maskPattern = "8 (***) *** **-**",
            placeholder = '*',
            formatPattern = null
        )
        state = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.phone(),
            formatter = formatter
        )
    }

    @Test
    fun `typing updates masked text and advances caret`() {
        state.updateValue("123")
        // Text masked correctly
        assertEquals("8 (123)       -  ", state.textFieldValue.text)
        // After three digits caret should be at next group start
        val nextGroupStart = formatter.validPositions[3]
        assertEquals(nextGroupStart, state.textFieldValue.selection.start)
        assertEquals(InputState.PARTIAL, state.currentState)
    }

    @Test
    fun `deletion moves caret back`() {
        state.updateValue("1234567")
        val before = state.textFieldValue.selection.start
        state.updateValue("123456")
        val after = state.textFieldValue.selection.start
        assertEquals(before - 1, after)
    }

    @Test
    fun `complete input moves caret to trailing`() {
        state.updateValue("1234567890")
        val trailing = (formatter.lastValidPosition()!! + 1)
            .coerceAtMost(state.textFieldValue.text.length)
        assertEquals(trailing, state.textFieldValue.selection.start)
        assertEquals(InputState.COMPLETE, state.currentState)
    }

    @Test
    fun `formatting returns expected value`() {
        val ccFormatter = MaskFormatter(
            maskPattern = "**** **** **** ****",
            placeholder = '*',
            formatPattern = "[1][2][3][4]-[5][6][7][8]-[9][10][11][12]-[13][14][15][16]"
        )
        val ccState = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**** **** **** ****"),
            formatter = ccFormatter
        )
        ccState.updateValue("1234567890123456")
        assertEquals("1234-5678-9012-3456", ccState.getFormattedValue())
    }

    @Test
    fun `paste middle area replaces and sets caret after inserted`() {
        val hyphenFormatter = MaskFormatter(
            maskPattern = "**-**-**",
            placeholder = '*',
            formatPattern = null
        )
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = hyphenFormatter
        )
        // Simulate paste by setting full new unmasked value
        st.updateValue("1234") // becomes 12-34-  ; length = 4
        // Expect caret at end of inserted digits (index 4 in validPositions)
        val expected = hyphenFormatter.validPositions[4]
        assertEquals(expected, st.textFieldValue.selection.start)
    }

    @Test
    fun `delete across literal moves caret correctly`() {
        val hyphenFormatter = MaskFormatter(
            maskPattern = "**-**-**",
            placeholder = '*',
            formatPattern = null
        )
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = hyphenFormatter
        )
        st.updateValue("123456")
        // Now delete last digit -> 5 digits
        st.updateValue("12345")
        val expectedCaret = hyphenFormatter.validPositions[5] // 6th slot
        assertEquals(expectedCaret, st.textFieldValue.selection.start)
    }

    @Test
    fun `overflow input keeps trailing caret and truncates`() {
        val hyphenFormatter = MaskFormatter(
            maskPattern = "**-**-**",
            placeholder = '*',
            formatPattern = null
        )
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = hyphenFormatter
        )
        st.updateValue("1234567890") // longer than 6 slots
        val trailing = (hyphenFormatter.lastValidPosition()!! + 1)
            .coerceAtMost(st.textFieldValue.text.length)
        assertEquals(trailing, st.textFieldValue.selection.start)
        // Normalized unmasked length equals slot count (6)
        val normalized = hyphenFormatter.normalize(st.unmaskedValue)
        assertEquals(6, normalized.length)
    }

    @Test
    fun `insert then delete within middle keeps caret at group boundary`() {
        val fmt = MaskFormatter("**-**-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = fmt
        )
        // Two groups
        st.updateValue("1234")
        // After four digits, caret should be at the start of the next group
        assertEquals(fmt.validPositions[4], st.textFieldValue.selection.start)
        // Replace middle logically: simulate change of unmasked to 1274
        st.updateValue("1274")
        // Caret remains at end of second group
        assertEquals(fmt.validPositions[4], st.textFieldValue.selection.start)
    }

    @Test
    fun `delete at group boundary moves caret back one slot`() {
        val fmt = MaskFormatter("**-**-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = fmt
        )
        st.updateValue("1234")
        val before = st.textFieldValue.selection.start
        st.updateValue("123")
        val after = st.textFieldValue.selection.start
        assertEquals(before - 1, after)
    }
}
