package com.github.pinball83.maskededittext

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import android.view.inputmethod.EditorInfo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class MaskedEditTextDeletionTest {

    private lateinit var context: Context
    private lateinit var editText: MaskedEditText

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        editText = MaskedEditText(context).apply {
            setMask("8 (***) *** **-**")
            setNotMaskedSymbol("*")
            setMaskedText("9261234567")
        }
    }

    @Test
    fun `backspace over mask removes preceding digit`() {
        // Cursor right after closing parenthesis = index 7; simulate backspace deleting index 6.
        val info = EditorInfo()
        val connection = editText.onCreateInputConnection(info)
        check(connection != null)

        editText.setSelection(7)
        val success = connection.deleteSurroundingText(1, 0)
        assertThat(success, equalTo(true))

        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val actual = editText.text.toString()
        assertThat(actual, equalTo("8 (921) 234 56-7 "))
    }

    @Test
    fun `forward delete across mask advances caret`() {
        val simple = MaskedEditText(context).apply {
            setMask("***-***")
            setNotMaskedSymbol("*")
            setMaskedText("123456")
        }
        val info = EditorInfo()
        val connection = simple.onCreateInputConnection(info)
        check(connection != null)

        simple.setSelection(3)
        connection.deleteSurroundingText(0, 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertThat(simple.text.toString(), equalTo("123-56 "))
        assertThat(simple.selectionStart, equalTo(5))
    }

    @Test
    fun `selection before leading literal snaps to first slot`() {
        val simple = MaskedEditText(context).apply {
            setMask("+7 (***)")
            setNotMaskedSymbol("*")
            setMaskedText("123")
        }

        simple.setSelection(2) // position before the first placeholder
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertThat(simple.selectionStart, equalTo(4))
    }
}
