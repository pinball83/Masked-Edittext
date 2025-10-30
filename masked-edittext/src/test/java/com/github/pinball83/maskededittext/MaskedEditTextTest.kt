package com.github.pinball83.maskededittext

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MaskedEditTextTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `setMaskedText applies mask and exposes unmasked text`() {
        val editText = MaskedEditText.Builder(context)
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .build()

        editText.setMaskedText("1234567890")

        assertEquals("8 (123) 456 78-90", editText.text.toString())
        assertEquals("1234567890", editText.getUnmaskedText().trim())
        assertTrue(editText.isInputComplete())
    }

    @Test
    fun `getFormattedText returns formatted value when pattern supplied`() {
        val editText = MaskedEditText.Builder(context)
            .mask("**** **** **** ****")
            .notMaskedSymbol("*")
            .format("[1][2][3][4]-[5][6][7][8]-[9][10][11][12]-[13][14][15][16]")
            .build()

        editText.setMaskedText("1234567890123456")

        assertEquals("1234567890123456", editText.getUnmaskedText().trim())
        assertEquals("1234-5678-9012-3456", editText.getFormattedText())
    }

    @Test
    fun `clearMaskedText resets widget`() {
        val editText = MaskedEditText.Builder(context)
            .mask("***-***")
            .notMaskedSymbol("*")
            .build()

        editText.setMaskedText("123456")
        assertFalse(editText.isInputEmpty())

        editText.clearMaskedText()
        assertTrue(editText.isInputEmpty())
        assertEquals("   -   ", editText.text.toString())
    }

    @Test
    fun `cursor advances to next valid position on setMaskedText`() {
        val editText = MaskedEditText.Builder(context)
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .build()

        // Initially at the first input slot
        assertEquals(3, editText.selectionStart)

        // After typing 3 digits -> next block's first slot
        editText.setMaskedText("123")
        assertEquals(8, editText.selectionStart)

        // After typing 7 digits -> before dash "8 (123) 456 7"
        editText.setMaskedText("1234567")
        assertEquals(13, editText.selectionStart)

        // After completing 10 digits -> last slot
        editText.setMaskedText("1234567890")
        assertEquals(16, editText.selectionStart)
    }

    @Test
    fun `cursor moves back on deletion with setMaskedText`() {
        val editText = MaskedEditText.Builder(context)
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .build()

        editText.setMaskedText("1234567")
        assertEquals(13, editText.selectionStart)

        // Simulate deletion: 7 -> 6 digits
        editText.setMaskedText("123456")
        assertEquals(12, editText.selectionStart)
    }
}
