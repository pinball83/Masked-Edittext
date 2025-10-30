package com.github.pinball83.maskededittext

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MaskedEditTextSetTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `setMaskedText applies mask correctly`() {
        val direct = MaskedEditText(context)
        direct.setMask("8 (***) *** **-**")
        direct.setNotMaskedSymbol("*")

        direct.setMaskedText("9261234567")

        assertThat(direct.text.toString(), equalTo("8 (926) 123 45-67"))

        val built = MaskedEditText.Builder(context)
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .build()

        built.setMaskedText("9261234567")
        assertThat(built.text.toString(), equalTo("8 (926) 123 45-67"))
    }
}
