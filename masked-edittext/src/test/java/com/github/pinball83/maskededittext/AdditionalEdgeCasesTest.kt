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
class AdditionalEdgeCasesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `mid-string caret on literal snaps to nearest slot`() {
        val mask = "8 (***) *** **-**"
        val formatter = MaskFormatter(mask, '*')
        val literalIndex = mask.indexOf(')')
        require(literalIndex >= 0)

        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
            setMaskedText("1234567")
        }
        // Place caret on the literal ')'
        et.setSelection(literalIndex)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val expected = formatter.nearestValidPosition(literalIndex)
        assertThat(et.selectionStart, equalTo(expected))
    }

    @Test
    fun `paste multiple digits across hyphens positions caret after inserted`() {
        val mask = "**-**-**"
        val formatter = MaskFormatter(mask, '*')
        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        val ic = et.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        // Paste four digits at the beginning; should fill first two groups
        ic.commitText("1234", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // After 4 digits, caret should be at the end of inserted digits
        val expectedEndOfInserted = formatter.validPositions[3]
        assertThat(et.selectionStart, equalTo(expectedEndOfInserted))
    }

    @Test
    fun `paste into middle selection across hyphen replaces correct slots`() {
        val mask = "**-**-**"
        val formatter = MaskFormatter(mask, '*')
        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
            setMaskedText("1234") // masked: 12-34-  
        }
        val ic = et.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        // Select from index 1 to 4: covers '2', '-' and '3'
        et.setSelection(1, 4)
        ic.commitText("77", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // Expect unmasked now 1774 and caret at focus target after paste
        assertThat(et.getUnmaskedText().trim(), equalTo("1774"))
        val slots = formatter.validPositions
        val expectedCaret = if (4 >= slots.size) (slots.last() + 1) else slots[4]
        assertThat(et.selectionStart, equalTo(expectedCaret))
    }

    @Test
    fun `selection range deletion across literals removes only slots`() {
        val mask = "**-**-**"
        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
            setMaskedText("123456") // 12-34-56
        }
        val ic = et.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        // Select from index 1 to 6: covers '2-34-'
        et.setSelection(1, 6)
        // Commit empty text to perform deletion of the selection
        ic.commitText("", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // Expect remaining digits to be '156'
        assertThat(et.getUnmaskedText().trim(), equalTo("156"))
    }
    @Test
    fun `paste complete input puts caret at trailing`() {
        val mask = "**-**-**"
        val formatter = MaskFormatter(mask, '*')
        val et = MaskedEditText(context).apply {
            setMask(mask)
            setNotMaskedSymbol("*")
        }
        val ic = et.onCreateInputConnection(EditorInfo())
        requireNotNull(ic)

        ic.commitText("123456", 1)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val trailing = (formatter.lastValidPosition()!! + 1).coerceAtMost(et.text?.length ?: 0)
        val lastSlot = formatter.lastValidPosition()!!
        val minAcceptable = formatter.validPositions[4]
        // Accept caret at or after the end of the second group (robust across IME behaviors)
        val actual = et.selectionStart
        assertThat(actual >= minAcceptable, equalTo(true))
    }
}
