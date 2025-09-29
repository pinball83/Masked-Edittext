package com.github.pinball83.maskededittext.compose

import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import com.github.pinball83.maskededittext.InputEvent
import com.github.pinball83.maskededittext.InputState
import com.github.pinball83.maskededittext.InputStateMachine

/**
 * State holder for MaskedTextField with integrated state machine
 */
@Stable
class MaskedTextFieldState(
    initialValue: String = "",
    private val maskedOptions: MaskedOptions
) {
    private val maskProcessor = MaskProcessor(
        maskedOptions.mask, 
        maskedOptions.notMaskedSymbol, 
        maskedOptions.format
    )
    
    private val stateMachine = InputStateMachine(
        maskedEditText = null,
        stateListener = object : InputStateMachine.InputStateListener {
            override fun onStateChanged(oldState: InputState, newState: InputState, event: InputEvent) {
                maskedOptions.onStateChanged?.invoke(oldState, newState, event)
            }
        }
    )
    
    var textFieldValue by mutableStateOf(
        TextFieldValue(maskProcessor.applyMask(initialValue))
    )
        private set
    
    var unmaskedValue by mutableStateOf(initialValue)
        private set
    
    val currentState: InputState
        get() = stateMachine.getCurrentState()
    
    val isComplete: Boolean
        get() = stateMachine.isComplete()
    
    val isValid: Boolean
        get() = stateMachine.isValid()
    
    val isEmpty: Boolean
        get() = stateMachine.isEmpty()
    
    fun updateValue(newValue: TextFieldValue) {
        val unmasked = maskProcessor.removeMask(newValue.text)
        val masked = maskProcessor.applyMask(unmasked)
        
        textFieldValue = newValue.copy(text = masked)
        unmaskedValue = unmasked
        
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED)
    }
    
    fun updateValue(newValue: String) {
        val masked = maskProcessor.applyMask(newValue)
        textFieldValue = TextFieldValue(masked)
        unmaskedValue = newValue
        
        stateMachine.processEvent(InputEvent.TEXT_SET)
    }
    
    fun onFocusChanged(focused: Boolean) {
        if (focused) {
            stateMachine.processEvent(InputEvent.FOCUS_GAINED)
        } else {
            stateMachine.processEvent(InputEvent.FOCUS_LOST)
        }
    }
    
    fun clear() {
        textFieldValue = TextFieldValue(maskProcessor.applyMask(""))
        unmaskedValue = ""
        stateMachine.processEvent(InputEvent.INPUT_CLEARED)
    }
    
    fun validate() {
        stateMachine.processEvent(InputEvent.VALIDATE)
    }
    
    fun getFormattedValue(): String {
        return if (maskedOptions.format != null) {
            maskProcessor.formatOutput(unmaskedValue)
        } else {
            unmaskedValue
        }
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
}

/**
 * Creates and remembers a MaskedTextFieldState
 */
@Composable
fun rememberMaskedTextFieldState(
    initialValue: String = "",
    maskedOptions: MaskedOptions
): MaskedTextFieldState {
    return remember(maskedOptions) {
        MaskedTextFieldState(
            initialValue = initialValue,
            maskedOptions = maskedOptions
        )
    }
}