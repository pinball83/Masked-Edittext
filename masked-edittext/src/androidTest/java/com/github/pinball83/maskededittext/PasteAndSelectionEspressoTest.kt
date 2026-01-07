package com.github.pinball83.maskededittext

import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasteAndSelectionEspressoTest {

    @Test
    fun paste_multiple_digits_across_hyphens_positions_caret_after_inserted() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("**-**-**")
                act.masked.setNotMaskedSymbol("*")
                val ic = act.masked.onCreateInputConnection(EditorInfo())
                requireNotNull(ic)
                ic.commitText("1234", 1)

                val fmt = MaskFormatter("**-**-**", '*')
                val expectedEnd = fmt.validPositions[3]
                assertThat(act.masked.selectionStart, equalTo(expectedEnd))
            }
        }
    }

    @Test
    fun paste_complete_input_puts_caret_at_or_beyond_group_end() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("**-**-**")
                act.masked.setNotMaskedSymbol("*")
                val ic = act.masked.onCreateInputConnection(EditorInfo())
                requireNotNull(ic)
                ic.commitText("123456", 1)

                val fmt = MaskFormatter("**-**-**", '*')
                val trailing = (fmt.lastValidPosition()!! + 1).coerceAtMost(act.masked.text?.length ?: 0)
                val lastSlot = fmt.lastValidPosition()!!
                val minAcceptable = fmt.validPositions[4]
                val actual = act.masked.selectionStart
                assertThat(actual >= minAcceptable && actual <= trailing, equalTo(true))
            }
        }
    }

    @Test
    fun selection_range_deletion_across_literals_removes_only_slots() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("**-**-**")
                act.masked.setNotMaskedSymbol("*")
                act.masked.setMaskedText("123456") // 12-34-56
                // Select from index 1 to 6: covers '2-34-'
                act.masked.setSelection(1, 6)
                val ic = act.masked.onCreateInputConnection(EditorInfo())
                requireNotNull(ic)
                ic.commitText("", 1) // replace selection with empty

                assertThat(act.masked.getUnmaskedText().trim(), equalTo("156"))
            }
        }
    }
}

