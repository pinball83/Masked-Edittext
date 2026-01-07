package com.github.pinball83.maskededittext

import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultiLiteralEspressoTest {

    @Test
    fun typing_across_multi_literal_run_advances_caret() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("***))  ***")
                act.masked.setNotMaskedSymbol("*")
            }
            scenario.onActivity { act ->
                val ic = act.masked.onCreateInputConnection(EditorInfo())
                requireNotNull(ic)
                ic.commitText("1", 1)
                ic.commitText("2", 1)
                ic.commitText("3", 1)
                val fmt = MaskFormatter("***))  ***", '*')
                val expected = fmt.validPositions[3]
                assertThat(act.masked.selectionStart, equalTo(expected))
            }
        }
    }

    @Test
    fun typing_across_mixed_literals_advances_caret() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.masked.setMask("**- ** -**")
                act.masked.setNotMaskedSymbol("*")
                val ic = act.masked.onCreateInputConnection(EditorInfo())
                requireNotNull(ic)
                ic.commitText("1", 1)
                ic.commitText("2", 1)
                val fmt = MaskFormatter("**- ** -**", '*')
                val expected = fmt.validPositions[2]
                assertThat(act.masked.selectionStart, equalTo(expected))
            }
        }
    }
}

