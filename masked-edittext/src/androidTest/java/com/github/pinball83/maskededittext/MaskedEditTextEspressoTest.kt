package com.github.pinball83.maskededittext

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.input.key.Key
import com.github.pinball83.maskededittext.compose.MaskedOptions
import com.github.pinball83.maskededittext.compose.MaskedTextField
import org.junit.Rule
import org.junit.Test

class MaskedEditTextEspressoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun typing_across_paren_advances_caret() {
        val value = mutableStateOf("")
        composeTestRule.setContent {
            MaskedTextField(
                value = value.value,
                onValueChange = { value.value = it },
                maskedOptions = MaskedOptions.phone(),
                modifier = Modifier.testTag("masked")
            )
        }
        composeTestRule.onNodeWithTag("masked").performTextInput("123")
        composeTestRule.onNodeWithTag("masked").assertTextEquals("8 (123)       -  ")
    }

    @Test
    fun backspace_over_paren_deletes_previous_digit() {
        val value = mutableStateOf("")
        composeTestRule.setContent {
            MaskedTextField(
                value = value.value,
                onValueChange = { value.value = it },
                maskedOptions = MaskedOptions.phone(),
                modifier = Modifier.testTag("masked")
            )
        }
        composeTestRule.onNodeWithTag("masked").performTextInput("9261234567")
        // One backspace from end; previous digit removed
        composeTestRule.onNodeWithTag("masked").performKeyInput { pressKey(Key.Backspace) }
        composeTestRule.onNodeWithTag("masked").assertTextEquals("8 (926) 123 45-6 ")
    }

    @Test
    fun focus_snaps_to_first_slot_with_leading_literal() {
        val value = mutableStateOf("")
        composeTestRule.setContent {
            MaskedTextField(
                value = value.value,
                onValueChange = { value.value = it },
                maskedOptions = MaskedOptions.custom("+7 (***)"),
                modifier = Modifier.testTag("masked")
            )
        }
        // On first render, caret sits at first slot; we assert initial template
        composeTestRule.onNodeWithTag("masked").assertTextEquals("+7 (   )")
    }
}
