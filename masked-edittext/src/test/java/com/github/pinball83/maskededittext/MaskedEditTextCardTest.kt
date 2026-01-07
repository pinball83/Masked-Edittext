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

@RunWith(RobolectricTestRunner::class)
class MaskedEditTextCardTest {

    private lateinit var context: Context
    private lateinit var editText: MaskedEditText

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        editText = MaskedEditText(context).apply {
            setMask("**** **** **** ****")
            setNotMaskedSymbol("*")
        }
    }

    @Test
    fun `focus positions cursor at first slot`() {
        val info = EditorInfo()
        editText.onCreateInputConnection(info)
        editText.requestFocus()
        editText.onFocusChange(editText, true)
        assertThat(editText.selectionStart, equalTo(0))
    }

    @Test
    fun `typing updates mask without crash`() {
        val connection = editText.onCreateInputConnection(EditorInfo())
        requireNotNull(connection)

        connection.commitText("1234", 1)
        println("after commit='${editText.text}'")
        assertThat(editText.text.toString().take(4), equalTo("1234"))

        connection.deleteSurroundingText(1, 0)
        println("after delete='${editText.text}'")
        assertThat(editText.text.toString().take(3), equalTo("123"))
    }
}
