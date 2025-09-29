package com.github.pinball83.maskededittext.compose

import com.github.pinball83.maskededittext.InputEvent
import com.github.pinball83.maskededittext.InputState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for MaskedTextFieldState
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = [33])
class MaskedTextFieldStateTest {

    @Mock
    private lateinit var mockStateListener: (InputState, InputState, InputEvent) -> Unit

    private lateinit var phoneState: MaskedTextFieldState
    private lateinit var cardState: MaskedTextFieldState

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        phoneState = MaskedTextFieldState(
            maskedOptions = MaskedOptions.phone(onStateChanged = mockStateListener)
        )
        
        cardState = MaskedTextFieldState(
            maskedOptions = MaskedOptions.creditCard()
        )
    }

    @Test
    fun testInitialState() {
        assertEquals(InputState.EMPTY, phoneState.currentState)
        assertTrue(phoneState.isEmpty)
        assertFalse(phoneState.isComplete)
        assertTrue(phoneState.isValid)
        assertEquals("", phoneState.unmaskedValue)
        assertEquals("8 (   )    --  ", phoneState.textFieldValue.text)
    }

    @Test
    fun testUpdateValueWithString() {
        phoneState.updateValue("1234567890")
        
        assertEquals("1234567890", phoneState.unmaskedValue)
        assertEquals("8 (123) 456 78-90", phoneState.textFieldValue.text)
        assertEquals(InputState.COMPLETE, phoneState.currentState)
        assertTrue(phoneState.isComplete)
    }

    @Test
    fun testUpdateValueWithPartialInput() {
        phoneState.updateValue("12345")
        
        assertEquals("12345", phoneState.unmaskedValue)
        assertEquals("8 (123) 45 --  ", phoneState.textFieldValue.text)
        assertEquals(InputState.PARTIAL, phoneState.currentState)
        assertFalse(phoneState.isComplete)
    }

    @Test
    fun testClearFunction() {
        phoneState.updateValue("1234567890")
        phoneState.clear()
        
        assertEquals("", phoneState.unmaskedValue)
        assertEquals("8 (   )    --  ", phoneState.textFieldValue.text)
        assertEquals(InputState.EMPTY, phoneState.currentState)
        assertTrue(phoneState.isEmpty)
    }

    @Test
    fun testFocusEvents() {
        phoneState.onFocusChanged(true)
        assertEquals(InputState.FOCUSED, phoneState.currentState)
        
        phoneState.onFocusChanged(false)
        assertEquals(InputState.UNFOCUSED, phoneState.currentState)
    }

    @Test
    fun testValidateFunction() {
        phoneState.updateValue("12345")
        phoneState.validate()
        
        // Should remain in PARTIAL state for partial input
        assertEquals(InputState.PARTIAL, phoneState.currentState)
    }

    @Test
    fun testCreditCardMask() {
        cardState.updateValue("1234567890123456")
        
        assertEquals("1234567890123456", cardState.unmaskedValue)
        assertEquals("1234 5678 9012 3456", cardState.textFieldValue.text)
        assertTrue(cardState.isComplete)
    }

    @Test
    fun testCustomFormat() {
        val customState = MaskedTextFieldState(
            mask = "8 (***) *** **-**",
            notMaskedSymbol = '*',
            format = "[1][2][3] [4][5][6]-[7][8]-[10][9]"
        )
        
        customState.updateValue("1234567890")
        
        assertEquals("1234567890", customState.unmaskedValue)
        assertEquals("123 456-78-09", customState.getFormattedValue())
    }

    @Test
    fun testStateChangeNotification() {
        phoneState.updateValue("1")
        
        verify(mockStateListener).invoke(InputState.EMPTY, InputState.PARTIAL, InputEvent.TEXT_SET)
    }

    @Test
    fun testLongInput() {
        // Input longer than mask should be truncated
        cardState.updateValue("12345678901234567890")
        
        assertEquals("1234567890123456", cardState.unmaskedValue)
        assertEquals("1234 5678 9012 3456", cardState.textFieldValue.text)
    }

    @Test
    fun testEmptyMask() {
        val noMaskState = MaskedTextFieldState(
            mask = "",
            notMaskedSymbol = '*'
        )
        
        noMaskState.updateValue("test input")
        
        assertEquals("test input", noMaskState.unmaskedValue)
        assertEquals("test input", noMaskState.textFieldValue.text)
    }

    @Test
    fun testSpecialCharacterMask() {
        val specialState = MaskedTextFieldState(
            mask = "Q***************",
            notMaskedSymbol = '*'
        )
        
        specialState.updateValue("ABCDEFGHIJKLMNO")
        
        assertEquals("ABCDEFGHIJKLMNO", specialState.unmaskedValue)
        assertEquals("QABCDEFGHIJKLMNO", specialState.textFieldValue.text)
    }

    @Test
    fun testMixedMask() {
        val mixedState = MaskedTextFieldState(
            mask = "TSH***************",
            notMaskedSymbol = '*'
        )
        
        mixedState.updateValue("123456789012345")
        
        assertEquals("123456789012345", mixedState.unmaskedValue)
        assertEquals("TSH123456789012345", mixedState.textFieldValue.text)
    }
}