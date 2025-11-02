package com.github.pinball83.maskededittext

import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class MultiLiteralCaretPolicyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `typing across multi literal run '))  ' advances caret to next slot`() {
        val mask = "***))  ***"
        val formatter = MaskFormatter(mask, '*')
        val nextSlotIndexAfterThree = formatter.validPositions[3]

        val editText = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        val ic = editText.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        ic.commitText("1", 1)
        ic.commitText("2", 1)
        ic.commitText("3", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // Caret should be at the first slot of the second group (after "))  ")
        assertThat(editText.selectionStart, equalTo(nextSlotIndexAfterThree))
    }

    @Test
    fun `backspace from next group deletes previous digit across multi literals`() {
        val mask = "***))  ***"
        val editText = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
            setMaskedText("123456")
        }
        val ic = editText.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        // Place caret at the start of the second group and backspace
        val formatter = MaskFormatter(mask, '*')
        val startSecondGroup = formatter.validPositions[3]
        editText.setSelection(startSecondGroup)

        ic.deleteSurroundingText(1, 0)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // Unmasked should drop the third digit ('3'): 123456 -> 12456
        assertThat(editText.getUnmaskedText().trim(), equalTo("12456"))
    }

    @Test
    fun `typing across mixed literals ' - ' advances caret`() {
        val mask = "**- ** -**"
        val formatter = MaskFormatter(mask, '*')
        val nextSlotIndexAfterTwo = formatter.validPositions[2]

        val editText = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        val ic = editText.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        ic.commitText("1", 1)
        ic.commitText("2", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // After two digits, caret should skip "- " and land on next slot in the second group
        assertThat(editText.selectionStart, equalTo(nextSlotIndexAfterTwo))
    }
}
