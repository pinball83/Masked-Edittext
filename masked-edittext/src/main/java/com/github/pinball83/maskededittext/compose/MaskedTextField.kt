package com.github.pinball83.maskededittext.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.pinball83.maskededittext.InputEvent
import com.github.pinball83.maskededittext.InputState
import com.github.pinball83.maskededittext.InputStateMachine

/**
 * A Jetpack Compose implementation of MaskedEditText with state machine support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    maskedOptions: MaskedOptions,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    visualOptions: MaskedVisualOptions = MaskedVisualOptions(),
    inputOptions: MaskedInputOptions = MaskedInputOptions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var maskedText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    
    // Create mask processor
    val maskProcessor = remember(maskedOptions.mask, maskedOptions.notMaskedSymbol) {
        MaskProcessor(maskedOptions.mask, maskedOptions.notMaskedSymbol, maskedOptions.format)
    }
    
    // State machine for input validation
    val stateMachine = remember {
        InputStateMachine(
            maskedEditText = null, // We'll handle validation differently in Compose
            stateListener = object : InputStateMachine.InputStateListener {
                override fun onStateChanged(oldState: InputState, newState: InputState, event: InputEvent) {
                    maskedOptions.onStateChanged?.invoke(oldState, newState, event)
                }
            }
        )
    }
    
    // Update masked text when value changes
    LaunchedEffect(value, maskedOptions.mask, maskedOptions.notMaskedSymbol) {
        val processed = maskProcessor.applyMask(value)
        maskedText = processed
        textFieldValue = TextFieldValue(processed, textFieldValue.selection)
    }
    
    // Handle text changes
    val handleTextChange: (TextFieldValue) -> Unit = { newValue ->
        val unmasked = maskProcessor.removeMask(newValue.text)
        val masked = maskProcessor.applyMask(unmasked)
        
        textFieldValue = newValue.copy(text = masked)
        maskedText = masked
        onValueChange(unmasked)
        
        // Trigger state machine event
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED)
    }
    
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = handleTextChange,
        modifier = modifier
            .onFocusChanged { focusState ->
                val wasFocused = isFocused
                isFocused = focusState.isFocused
                
                if (!wasFocused && focusState.isFocused) {
                    stateMachine.processEvent(InputEvent.FOCUS_GAINED)
                } else if (wasFocused && !focusState.isFocused) {
                    stateMachine.processEvent(InputEvent.FOCUS_LOST)
                }
            },
        enabled = visualOptions.enabled,
        readOnly = visualOptions.readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon?.let { icon ->
            {
                Row {
                    icon()
                    maskedOptions.onIconClick?.let { callback ->
                        IconButton(
                            onClick = { callback(maskProcessor.removeMask(maskedText)) }
                        ) {
                            // Default clear icon or custom icon
                        }
                    }
                }
            }
        },
        supportingText = supportingText,
        isError = visualOptions.isError,
        keyboardOptions = inputOptions.keyboardOptions,
        keyboardActions = inputOptions.keyboardActions,
        singleLine = visualOptions.singleLine,
        maxLines = visualOptions.maxLines,
        minLines = visualOptions.minLines,
        interactionSource = interactionSource,
        colors = colors
    )
}

/**
 * Helper class to process mask operations
 */
private class MaskProcessor(
    private val mask: String,
    private val notMaskedSymbol: Char,
    private val format: String? = null
) {
    private val validPositions = mutableListOf<Int>()
    
    init {
        // Find valid cursor positions
        mask.forEachIndexed { index, char ->
            if (char == notMaskedSymbol) {
                validPositions.add(index)
            }
        }
    }
    
    fun applyMask(input: String): String {
        if (mask.isEmpty()) return input
        
        val result = StringBuilder(mask.replace(notMaskedSymbol, ' '))
        var inputIndex = 0
        
        for (position in validPositions) {
            if (inputIndex < input.length && position < result.length) {
                result[position] = input[inputIndex]
                inputIndex++
            }
        }
        
        return result.toString()
    }
    
    fun removeMask(maskedInput: String): String {
        if (mask.isEmpty()) return maskedInput
        
        val result = StringBuilder()
        
        for (position in validPositions) {
            if (position < maskedInput.length) {
                val char = maskedInput[position]
                if (char != ' ') {
                    result.append(char)
                }
            }
        }
        
        return result.toString()
    }
    
    fun formatOutput(unmaskedText: String): String {
        if (format.isNullOrEmpty()) return unmaskedText
        
        // Apply custom formatting if specified
        var result = format
        unmaskedText.forEachIndexed { index, char ->
            val placeholder = "[${index + 1}]"
            result = result.replace(placeholder, char.toString())
        }
        
        return result
    }
}

@Preview(showBackground = true)
@Composable
fun MaskedTextFieldPreview() {
    var phoneNumber by remember { mutableStateOf("") }
    
    MaterialTheme {
        MaskedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            maskedOptions = MaskedOptions.phone(),
            label = { Text("Phone Number") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            inputOptions = MaskedInputOptions(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreditCardMaskedTextFieldPreview() {
    var cardNumber by remember { mutableStateOf("") }
    
    MaterialTheme {
        MaskedTextField(
            value = cardNumber,
            onValueChange = { cardNumber = it },
            maskedOptions = MaskedOptions.creditCard(),
            label = { Text("Credit Card Number") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            inputOptions = MaskedInputOptions(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        )
    }
}