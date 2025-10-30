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
class MaskedEditTextTypingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `typing digits inserts into slots`() {
        val editText = MaskedEditText(context).apply {
            setMask("8 (***) *** **-**")
            setNotMaskedSymbol("*")
            setSelection(3)
        }

        val info = EditorInfo()
        val connection = editText.onCreateInputConnection(info)
        check(connection != null)

        connection.commitText("9", 1)
        assertThat(editText.text.toString(), equalTo("8 (9  )       -  "))
    }
}
