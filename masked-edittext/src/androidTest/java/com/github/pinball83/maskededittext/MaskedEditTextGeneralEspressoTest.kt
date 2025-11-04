package com.github.pinball83.maskededittext

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.pinball83.maskededittext.compose.MaskedOptions
import com.github.pinball83.maskededittext.compose.MaskedTextField
import org.junit.Rule
import org.junit.Test

class MaskedEditTextGeneralEspressoTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setValue_appliesMask_and_exposesFormatted() {
        val value = mutableStateOf("")
        composeRule.setContent {
            MaskedTextField(
                value = value.value,
                onValueChange = { value.value = it },
                maskedOptions = MaskedOptions.custom("**** **** **** ****"),
                modifier = Modifier.testTag("masked")
            )
        }
        composeRule.runOnIdle { value.value = "1234567890123456" }
        composeRule.onNodeWithTag("masked").assertTextEquals("1234 5678 9012 3456")
    }

    @Test
    fun clear_resets_template() {
        val value = mutableStateOf("")
        composeRule.setContent {
            MaskedTextField(
                value = value.value,
                onValueChange = { value.value = it },
                maskedOptions = MaskedOptions.custom("***-***"),
                modifier = Modifier.testTag("masked")
            )
        }
        composeRule.runOnIdle { value.value = "123456" }
        composeRule.onNodeWithTag("masked").assertTextEquals("123-456")
        composeRule.runOnIdle { value.value = "" }
        composeRule.onNodeWithTag("masked").assertTextEquals("   -   ")
    }
}
