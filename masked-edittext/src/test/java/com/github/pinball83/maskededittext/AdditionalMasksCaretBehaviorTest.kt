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
class AdditionalMasksCaretBehaviorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `date mask typing advances across slashes`() {
        val mask = "**/**/**"
        val formatter = MaskFormatter(mask, '*')
        val firstAfterTwo = formatter.validPositions[2]

        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        val ic = et.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        ic.commitText("1", 1)
        ic.commitText("2", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertThat(et.selectionStart, equalTo(firstAfterTwo))

        // Continue typing to cross second '/'
        val firstAfterFour = formatter.validPositions[4]
        ic.commitText("3", 1)
        ic.commitText("4", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertThat(et.selectionStart, equalTo(firstAfterFour))
    }

    @Test
    fun `time mask typing advances across colon`() {
        val mask = "**:**"
        val formatter = MaskFormatter(mask, '*')
        val afterTwo = formatter.validPositions[2]

        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        val ic = et.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        ic.commitText("1", 1)
        ic.commitText("2", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertThat(et.selectionStart, equalTo(afterTwo))
    }

    @Test
    fun `ssn mask typing advances across hyphens`() {
        val mask = "***-**-****"
        val formatter = MaskFormatter(mask, '*')

        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        val ic = et.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        ic.commitText("1", 1)
        ic.commitText("2", 1)
        ic.commitText("3", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // After 3 digits, caret should be at the next group start (skip '-')
        assertThat(et.selectionStart, equalTo(formatter.validPositions[3]))

        // After total 5 digits, caret should be at the third group start (skip second '-')
        ic.commitText("4", 1)
        ic.commitText("5", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertThat(et.selectionStart, equalTo(formatter.validPositions[5]))
    }

    @Test
    fun `leading literal focus snaps to first slot`() {
        val mask = "+7 (***)"
        val formatter = MaskFormatter(mask, '*')
        val firstSlot = formatter.firstValidPosition() ?: 0

        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        et.onCreateInputConnection(EditorInfo())
        et.requestFocus()
        et.onFocusChange(et, true)

        assertThat(et.selectionStart, equalTo(firstSlot))
    }

    @Test
    fun `trailing after complete input`() {
        val mask = "**/**/**"
        val formatter = MaskFormatter(mask, '*')
        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        et.onCreateInputConnection(EditorInfo())

        // Enter full 6 digits
        val ic = et.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)
        ic.commitText("1", 1)
        ic.commitText("2", 1)
        ic.commitText("3", 1)
        ic.commitText("4", 1)
        ic.commitText("5", 1)
        ic.commitText("6", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val trailing = (formatter.lastValidPosition()!! + 1).coerceAtMost(et.text?.length ?: 0)
        assertThat(et.selectionStart, equalTo(trailing))
    }
}

