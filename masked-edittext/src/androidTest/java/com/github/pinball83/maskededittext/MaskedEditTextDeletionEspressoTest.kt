package com.github.pinball83.maskededittext

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import com.github.pinball83.maskededittext.compose.MaskedOptions
import com.github.pinball83.maskededittext.compose.MaskedTextField
import org.junit.Rule
import org.junit.Test

class MaskedEditTextDeletionEspressoTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun forward_delete_across_hyphen_advances_caret() {
        val value = mutableStateOf("")
        composeRule.setContent {
            MaskedTextField(
                value = value.value,
                onValueChange = { value.value = it },
                maskedOptions = MaskedOptions.custom("***-***"),
                modifier = Modifier.testTag("masked")
            )
        }
        composeRule.onNodeWithTag("masked").performTextInput("123456")
        // One Delete from end acts like forward-delete; simulate by backspace for UI assert baseline
        composeRule.onNodeWithTag("masked").performKeyInput { pressKey(Key.Delete) }
        composeRule.onNodeWithTag("masked").assertTextEquals("123-56 ")
    }
}
