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

        assertThat(editText.text.toString(), equalTo("8 (921) 234 56-7 "))
    }
}
