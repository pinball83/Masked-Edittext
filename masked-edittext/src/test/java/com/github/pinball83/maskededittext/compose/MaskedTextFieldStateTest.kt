package com.github.pinball83.maskededittext.compose

import com.github.pinball83.maskededittext.InputState
import com.github.pinball83.maskededittext.MaskFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MaskedTextFieldStateTest {

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
    fun `update value applies mask and updates state`() {
        state.updateValue("1234567890")

        assertEquals("1234567890", state.unmaskedValue)
        assertEquals("8 (123) 456 78-90", state.textFieldValue.text)
        assertEquals(InputState.COMPLETE, state.currentState)
        assertTrue(state.isComplete)
    }

    @Test
    fun `partial value remains partial`() {
        state.updateValue("12345")

        assertEquals(InputState.PARTIAL, state.currentState)
        assertFalse(state.isComplete)
    }

    @Test
    fun `clear resets to empty state`() {
        state.updateValue("1234567890")
        state.clear()

        assertEquals(InputState.EMPTY, state.currentState)
        assertTrue(state.isEmpty)
    }
}
