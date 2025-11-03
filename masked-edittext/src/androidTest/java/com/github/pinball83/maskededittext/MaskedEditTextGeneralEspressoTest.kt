package com.github.pinball83.maskededittext

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaskedEditTextGeneralEspressoTest {

    @Test
    fun setMaskedText_appliesMask_and_exposesUnmaskedText() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                val editText = MaskedEditText.Builder(act)
                    .mask("8 (***) *** **-**")
                    .notMaskedSymbol("*")
                    .build()
                act.masked = editText
                (act.findViewById(android.R.id.content) as android.view.ViewGroup).removeAllViews()
                (act.findViewById(android.R.id.content) as android.view.ViewGroup).addView(editText)

                editText.setMaskedText("1234567890")
                assertThat(editText.text.toString(), equalTo("8 (123) 456 78-90"))
                assertThat(editText.getUnmaskedText().trim(), equalTo("1234567890"))
                assertThat(editText.isInputComplete(), equalTo(true))
            }
        }
    }

    @Test
    fun getFormattedText_returnsFormattedValue() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                val editText = MaskedEditText.Builder(act)
                    .mask("**** **** **** ****")
                    .notMaskedSymbol("*")
                    .format("[1][2][3][4]-[5][6][7][8]-[9][10][11][12]-[13][14][15][16]")
                    .build()
                act.masked = editText
                (act.findViewById(android.R.id.content) as android.view.ViewGroup).removeAllViews()
                (act.findViewById(android.R.id.content) as android.view.ViewGroup).addView(editText)

                editText.setMaskedText("1234567890123456")
                assertThat(editText.getUnmaskedText().trim(), equalTo("1234567890123456"))
                assertThat(editText.getFormattedText(), equalTo("1234-5678-9012-3456"))
            }
        }
    }

    @Test
    fun clearMaskedText_resetsWidget() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                val editText = MaskedEditText.Builder(act)
                    .mask("***-***")
                    .notMaskedSymbol("*")
                    .build()
                act.masked = editText
                (act.findViewById(android.R.id.content) as android.view.ViewGroup).removeAllViews()
                (act.findViewById(android.R.id.content) as android.view.ViewGroup).addView(editText)

                editText.setMaskedText("123456")
                assertThat(editText.isInputEmpty(), equalTo(false))
                editText.clearMaskedText()
                assertThat(editText.isInputEmpty(), equalTo(true))
                assertThat(editText.text.toString(), equalTo("   -   "))
            }
        }
    }

    @Test
    fun cursor_advances_on_setMaskedText_and_moves_back_on_delete() {
        ActivityScenario.launch(TestHostActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                val editText = MaskedEditText.Builder(act)
                    .mask("8 (***) *** **-**")
                    .notMaskedSymbol("*")
                    .build()
                act.masked = editText
                (act.findViewById(android.R.id.content) as android.view.ViewGroup).removeAllViews()
                (act.findViewById(android.R.id.content) as android.view.ViewGroup).addView(editText)

                // Initial first slot
                assertThat(editText.selectionStart, equalTo(3))
                editText.setMaskedText("123")
                assertThat(editText.selectionStart, equalTo(8))
                editText.setMaskedText("1234567")
                assertThat(editText.selectionStart, equalTo(13))
                editText.setMaskedText("1234567890")
                assertThat(editText.selectionStart, equalTo(17))

                editText.setMaskedText("1234567")
                assertThat(editText.selectionStart, equalTo(13))
                editText.setMaskedText("123456")
                assertThat(editText.selectionStart, equalTo(12))
            }
        }
    }
}

