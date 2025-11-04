package com.github.pinball83.maskededittext.compose

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
    fun `text field value typing crosses literal boundaries`() {
        val fmt = MaskFormatter("**-**-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = fmt
        )

        st.simulateImeInput('1')
        assertEquals(fmt.validPositions[1], st.textFieldValue.selection.start)

        st.simulateImeInput('2')
        assertEquals(fmt.validPositions[2], st.textFieldValue.selection.start)

        st.simulateImeInput('3')
        assertEquals(fmt.validPositions[3], st.textFieldValue.selection.start)
    }

    @Test
    fun `manual caret move over literal snaps forward`() {
        val fmt = MaskFormatter("**-**-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = fmt
        )

        st.updateValue("1234")
        val literalIndex = st.textFieldValue.text.indexOf('-')
        require(literalIndex >= 0)

        st.updateValue(TextFieldValue(st.textFieldValue.text, TextRange(literalIndex)))

        assertEquals(fmt.validPositions[2], st.textFieldValue.selection.start)
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
        assertEquals(fmt.cursorPositionFor(st.unmaskedValue.length), st.textFieldValue.selection.start)
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
        st.updateValue("123")
        val expected = fmt.cursorPositionFor(st.unmaskedValue.length)
        assertEquals(expected, st.textFieldValue.selection.start)
    }

    @Test
    fun `ime backspace across literal moves caret to previous slot`() {
        val fmt = MaskFormatter("**-**-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = fmt
        )

        st.updateValue("1234")
        st.simulateImeBackspace()

        val expected = fmt.cursorPositionFor(st.unmaskedValue.length)
        assertEquals(expected, st.textFieldValue.selection.start)
        assertEquals("123", fmt.normalize(st.unmaskedValue))
    }

    @Test
    fun `ime consecutive backspace skips multiple literals`() {
        val fmt = MaskFormatter("**-**-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**-**-**"),
            formatter = fmt
        )

        st.updateValue("12345")
        st.simulateImeBackspace() // remove 5
        st.simulateImeBackspace() // remove 4

        val expected = fmt.cursorPositionFor(st.unmaskedValue.length)
        assertEquals(expected, st.textFieldValue.selection.start)
        assertEquals("123", fmt.normalize(st.unmaskedValue))
    }

    @Test
    fun `ime backspace across phone space removes previous digit`() {
        val fmt = MaskFormatter("8 (***) *** **-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.phone(),
            formatter = fmt
        )

        st.updateValue("1234567")
        st.simulateImeBackspace()

        val normalized = fmt.normalize(st.unmaskedValue)
        assertEquals("123456", normalized)
        val expectedCaret = fmt.cursorPositionFor(normalized.length)
        assertEquals(expectedCaret, st.textFieldValue.selection.start)
    }

    @Test
    fun `ime backspace across phone parentheses removes previous digit`() {
        val fmt = MaskFormatter("8 (***) *** **-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.phone(),
            formatter = fmt
        )

        st.updateValue("123")
        st.simulateImeBackspace()

        val normalized = fmt.normalize(st.unmaskedValue)
        assertEquals("12", normalized)
        val expectedCaret = fmt.cursorPositionFor(normalized.length)
        assertEquals(expectedCaret, st.textFieldValue.selection.start)
    }

    @Test
    fun `ime typing after deletion fills next slot without artifacts`() {
        val fmt = MaskFormatter("8 (***) *** **-**", '*')
        val st = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.phone(),
            formatter = fmt
        )

        st.updateValue("1234567")
        st.simulateImeBackspace()

        // After deleting, typing a new digit should occupy the freed slot and preserve mask stability
        st.simulateImeInput('9')

        val normalized = fmt.normalize(st.unmaskedValue)
        assertEquals("1234569", normalized)
        assertEquals(fmt.mask(normalized), st.textFieldValue.text)
        val expectedCaret = fmt.cursorPositionFor(normalized.length)
        assertEquals(expectedCaret, st.textFieldValue.selection.start)
    }

    private fun MaskedTextFieldState.simulateImeInput(char: Char) {
        val current = textFieldValue
        val caret = current.selection.start
        val buffer = StringBuilder(current.text)
        if (caret in 0 until buffer.length) {
            buffer.setCharAt(caret, char)
        } else {
            buffer.append(char)
        }
        val nextCaret = (caret + 1).coerceAtMost(buffer.length)
        updateValue(TextFieldValue(buffer.toString(), TextRange(nextCaret)))
    }

    private fun MaskedTextFieldState.simulateImeBackspace() {
        val current = textFieldValue
        val caret = current.selection.start
        if (caret <= 0) return
        val buffer = StringBuilder(current.text)
        buffer.deleteCharAt(caret - 1)
        val newCaret = (caret - 1).coerceAtLeast(0)
        updateValue(TextFieldValue(buffer.toString(), TextRange(newCaret)))
    }
}
