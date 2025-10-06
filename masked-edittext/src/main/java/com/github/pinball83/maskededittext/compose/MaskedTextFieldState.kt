package com.github.pinball83.maskededittext.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.pinball83.maskededittext.InputEvent
import com.github.pinball83.maskededittext.InputState
import com.github.pinball83.maskededittext.InputStateMachine
import com.github.pinball83.maskededittext.MaskFormatter

/**
 * State holder for MaskedTextField with shared formatter and state machine support.
 */
@Stable
class MaskedTextFieldState internal constructor(
    initialValue: String,
    private val maskedOptions: MaskedOptions,
    private val formatter: MaskFormatter?
) {

    private var rawUnmasked by mutableStateOf(computeRaw(initialValue))

    private val stateMachine = InputStateMachine(
        maskedEditText = null,
        stateListener = object : InputStateMachine.InputStateListener {
            override fun onStateChanged(oldState: InputState, newState: InputState, event: InputEvent) {
                maskedOptions.onStateChanged?.invoke(oldState, newState, event)
            }
        }
    )

    init {
        stateMachine.setMaskEvaluator(object : InputStateMachine.MaskEvaluator {
            override fun currentUnmasked(): String = rawUnmasked
            override fun isComplete(unmasked: String): Boolean? = formatter?.isComplete(unmasked)
            override fun isValid(unmasked: String): Boolean? = null
        })
        if (initialValue.isNotEmpty()) {
            stateMachine.processEvent(InputEvent.TEXT_SET)
        }
    }

    var textFieldValue by mutableStateOf(createTextFieldValue(rawUnmasked))
        private set

    var unmaskedValue by mutableStateOf(formatter?.normalize(initialValue) ?: initialValue)
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
        val previousRaw = rawUnmasked
        val normalized = formatNormalized(newValue.text)
        rawUnmasked = computeRawFromNormalized(normalized)
        unmaskedValue = normalized

        val masked = formatter?.mask(normalized) ?: normalized
        textFieldValue = newValue.copy(text = masked, selection = TextRange(masked.length))

        stateMachine.processEvent(resolveEvent(previousRaw, rawUnmasked))
    }

    fun updateValue(newValue: String) {
        val normalized = formatNormalized(newValue)
        rawUnmasked = computeRawFromNormalized(normalized)
        unmaskedValue = normalized

        val masked = formatter?.mask(normalized) ?: normalized
        textFieldValue = TextFieldValue(masked, TextRange(masked.length))

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
        rawUnmasked = computeRaw("")
        unmaskedValue = ""
        val masked = formatter?.mask("") ?: ""
        textFieldValue = TextFieldValue(masked, TextRange(masked.length))
        stateMachine.processEvent(InputEvent.INPUT_CLEARED)
    }

    fun validate() {
        stateMachine.processEvent(InputEvent.VALIDATE)
    }

    fun getFormattedValue(): String {
        val formatted = formatter?.formatOutput(rawUnmasked)
        return formatted ?: unmaskedValue
    }

    private fun computeRaw(unmasked: String): String {
        val masked = formatter?.mask(unmasked) ?: unmasked
        return formatter?.unmask(masked) ?: masked
    }

    private fun computeRawFromNormalized(normalized: String): String {
        val masked = formatter?.mask(normalized) ?: normalized
        return formatter?.unmask(masked) ?: masked
    }

    private fun formatNormalized(input: String): String {
        return formatter?.normalize(input) ?: input
    }

    private fun createTextFieldValue(unmasked: String): TextFieldValue {
        val masked = formatter?.mask(unmasked) ?: unmasked
        return TextFieldValue(masked, TextRange(masked.length))
    }

    private fun resolveEvent(previousRaw: String, newRaw: String): InputEvent {
        return when {
            newRaw.length < previousRaw.length -> InputEvent.CHARACTER_DELETED
            newRaw.length - previousRaw.length > 1 -> InputEvent.TEXT_PASTED
            else -> InputEvent.CHARACTER_TYPED
        }
    }
}

@Composable
fun rememberMaskedTextFieldState(
    initialValue: String = "",
    maskedOptions: MaskedOptions
): MaskedTextFieldState {
    val formatter = remember(maskedOptions.mask, maskedOptions.notMaskedSymbol, maskedOptions.format) {
        maskedOptions.mask.takeIf { it.isNotEmpty() }?.let {
            MaskFormatter(it, maskedOptions.notMaskedSymbol, maskedOptions.format)
        }
    }
    return remember(maskedOptions, formatter, initialValue) {
        MaskedTextFieldState(
            initialValue = initialValue,
            maskedOptions = maskedOptions,
            formatter = formatter
        )
    }
}
