package com.github.pinball83.maskededittext.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.input.key.Key
import org.junit.Rule
import org.junit.Test

class MaskedTextFieldComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun phone_typing_jumps_over_paren_and_space() {
        val valueState = mutableStateOf("")
        composeTestRule.setContent {
            MaskedTextField(
                value = valueState.value,
                onValueChange = { valueState.value = it },
                maskedOptions = MaskedOptions.phone(),
                modifier = androidx.compose.ui.Modifier.testTag("masked")
            )
        }
        composeTestRule.onNodeWithTag("masked").performTextInput("123")
        // Expect masked template to reflect three digits in first group
        composeTestRule.onNodeWithTag("masked")
            .assertTextEquals("8 (123)       -  ")
    }

    @Test
    fun hyphen_backspace_deletes_previous_digit() {
        val valueState = mutableStateOf("")
        composeTestRule.setContent {
            MaskedTextField(
                value = valueState.value,
                onValueChange = { valueState.value = it },
                maskedOptions = MaskedOptions.custom("***-***"),
                modifier = androidx.compose.ui.Modifier.testTag("masked")
            )
        }
        composeTestRule.onNodeWithTag("masked").performTextInput("123456")
        // Backspace once
        composeTestRule.onNodeWithTag("masked").performKeyInput { pressKey(Key.Backspace) }
        // After one backspace from end, last digit removed
        composeTestRule.onNodeWithTag("masked")
            .assertTextEquals("123-45 ")
    }
}

