package com.github.pinball83.maskededittext

import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MaskedEditTextEspressoTest {

    @Test
    fun typing_across_paren_advances_caret() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("8 (***) *** **-**")
                act.masked.setNotMaskedSymbol("*")
                act.masked.requestFocus()
            }
            onView(withContentDescription("masked-under-test"))
                .perform(click(), typeText("123"))
            scenario.onActivity { act ->
                val fmt = MaskFormatter("8 (***) *** **-**", '*')
                // After 3 typed digits, caret should be at the first slot of the next group
                val expected = fmt.validPositions[3]
                assertThat(act.masked.selectionStart, equalTo(expected))
            }
        }
    }

    @Test
    fun backspace_over_paren_deletes_previous_digit() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("8 (***) *** **-**")
                act.masked.setNotMaskedSymbol("*")
                act.masked.setMaskedText("9261234567")
                // Place caret just after ')'
                act.masked.setSelection(7)
            }
            onView(withContentDescription("masked-under-test"))
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
            scenario.onActivity { act ->
                assertThat(act.masked.text.toString(), equalTo("8 (921) 234 56-7 "))
            }
        }
    }

    @Test
    fun focus_snaps_to_first_slot_with_leading_literal() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("+7 (***)")
                act.masked.setNotMaskedSymbol("*")
                act.masked.requestFocus()
                val fmt = MaskFormatter("+7 (***)", '*')
                val expected = fmt.firstValidPosition() ?: 0
                assertThat(act.masked.selectionStart, equalTo(expected))
            }
        }
    }
}

