package com.github.pinball83.maskededittext.compose

import com.github.pinball83.maskededittext.InputState
import com.github.pinball83.maskededittext.MaskFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class MaskedTextFieldDateTimeSsnComposeTest {

    @Test
    fun `date mask typing advances across slashes and completes at trailing`() {
        val fmt = MaskFormatter("**/**/**", '*')
        val state = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**/**/**"),
            formatter = fmt
        )

        state.updateValue("12")
        assertEquals(fmt.validPositions[2], state.textFieldValue.selection.start)
        assertEquals(InputState.PARTIAL, state.currentState)

        state.updateValue("1234")
        assertEquals(fmt.validPositions[4], state.textFieldValue.selection.start)

        state.updateValue("123456")
        val trailing = (fmt.lastValidPosition()!! + 1).coerceAtMost(state.textFieldValue.text.length)
        assertEquals(trailing, state.textFieldValue.selection.start)
        assertEquals(InputState.COMPLETE, state.currentState)
    }

    @Test
    fun `time mask typing advances across colon and completes`() {
        val fmt = MaskFormatter("**:**", '*')
        val state = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("**:**"),
            formatter = fmt
        )

        state.updateValue("12")
        assertEquals(fmt.validPositions[2], state.textFieldValue.selection.start)
        assertEquals(InputState.PARTIAL, state.currentState)

        state.updateValue("1234")
        val trailing = (fmt.lastValidPosition()!! + 1).coerceAtMost(state.textFieldValue.text.length)
        assertEquals(trailing, state.textFieldValue.selection.start)
        assertEquals(InputState.COMPLETE, state.currentState)
    }

    @Test
    fun `ssn mask typing advances across both hyphens and completes`() {
        val fmt = MaskFormatter("***-**-****", '*')
        val state = MaskedTextFieldState(
            initialValue = "",
            maskedOptions = MaskedOptions.custom("***-**-****"),
            formatter = fmt
        )

        state.updateValue("123")
        assertEquals(fmt.validPositions[3], state.textFieldValue.selection.start)

        state.updateValue("12345")
        assertEquals(fmt.validPositions[5], state.textFieldValue.selection.start)

        state.updateValue("1234567890")
        val trailing = (fmt.lastValidPosition()!! + 1).coerceAtMost(state.textFieldValue.text.length)
        assertEquals(trailing, state.textFieldValue.selection.start)
        assertEquals(InputState.COMPLETE, state.currentState)
    }
}

