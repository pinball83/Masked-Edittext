package com.github.pinball83.maskededittext

import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MaskedEditTextDeletionEspressoTest {

    @Test
    fun forward_delete_across_hyphen_advances_caret() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("***-***")
                act.masked.setNotMaskedSymbol("*")
                act.masked.setMaskedText("123456")
                // caret at 3 — before hyphen
                act.masked.setSelection(3)
            }
            onView(withContentDescription("masked-under-test"))
                .perform(pressKey(KeyEvent.KEYCODE_FORWARD_DEL))
            scenario.onActivity { act ->
                assertThat(act.masked.text.toString(), equalTo("123-56 "))
                assertThat(act.masked.selectionStart, equalTo(5))
            }
        }
    }
}

