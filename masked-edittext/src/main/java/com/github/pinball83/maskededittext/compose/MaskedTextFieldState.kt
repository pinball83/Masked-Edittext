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

    private fun MaskFormatter.cursorForLength(normalizedLength: Int, textLength: Int): Int {
        return when {
            normalizedLength <= 0 -> firstValidPosition() ?: 0
            normalizedLength >= validPositions.size -> {
                (lastValidPosition()?.plus(1))?.coerceAtMost(textLength) ?: textLength
            }
            else -> cursorPositionFor(normalizedLength)
        }.coerceIn(0, textLength)
    }

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
        val previousCaret = textFieldValue.selection.start
        val previousLen = unmaskedValue.length
        val previousMasked = textFieldValue.text
        val normalized = formatter?.let { fmt ->
            fmt.normalize(fmt.unmask(newValue.text))
        } ?: newValue.text
        rawUnmasked = computeRawFromNormalized(normalized)
        unmaskedValue = normalized

        val masked = formatter?.mask(normalized) ?: normalized
        val cursorPos = formatter?.let { fmt ->
            val maskedLength = masked.length
            val defaultPos = fmt.cursorForLength(normalized.length, maskedLength)
            val textChanged = masked != previousMasked
            val selectionChanged = newValue.selection != textFieldValue.selection
            // Deletion-aware caret policy: if we deleted and previous caret was at the
            // start of the next slot group, move back only one position to avoid
            // jumping across literals (e.g., hyphens).
            if (normalized.length < previousLen) {
                val slots = fmt.validPositions
                val prevWasNextSlotStart =
                    (normalized.length + 1) in slots.indices &&
                        previousCaret == slots[normalized.length + 1]
                val fallback = defaultPos.coerceIn(0, maskedLength)
                if (prevWasNextSlotStart) (previousCaret - 1).coerceAtLeast(0) else fallback
            } else if (textChanged) {
                defaultPos
            } else if (selectionChanged) {
                desiredSelectionFor(fmt, newValue.selection, maskedLength)
            } else {
                newValue.selection.start.coerceIn(0, maskedLength)
            }
        } ?: newValue.selection.start.coerceIn(0, masked.length)
        textFieldValue = newValue.copy(text = masked, selection = TextRange(cursorPos))

        if (rawUnmasked != previousRaw) {
            stateMachine.processEvent(resolveEvent(previousRaw, rawUnmasked))
        }
    }

    fun updateValue(newValue: String) {
        val previousRaw = rawUnmasked
        val previousCaret = textFieldValue.selection.start
        val previousLen = unmaskedValue.length

        val normalized = formatNormalized(newValue)
        rawUnmasked = normalized
        unmaskedValue = normalized

        val masked = formatter?.mask(normalized) ?: normalized
        val cursorPos = formatter?.let { fmt ->
            val defaultPos = fmt.cursorForLength(normalized.length, masked.length)
            if (normalized.length < previousLen) {
                val slots = fmt.validPositions
                val prevWasNextSlotStart =
                    (normalized.length + 1) in slots.indices &&
                        previousCaret == slots[normalized.length + 1]
                if (prevWasNextSlotStart) (previousCaret - 1).coerceAtLeast(0) else defaultPos
            } else {
                defaultPos
            }
        } ?: masked.length

        textFieldValue = TextFieldValue(masked, TextRange(cursorPos))

        if (rawUnmasked != previousRaw) {
            stateMachine.processEvent(InputEvent.TEXT_SET)
        }
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
        return formatter?.normalize(unmasked) ?: unmasked
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
        val normalizedLength = formatter?.normalize(unmasked)?.length ?: masked.length
        val cursorPos = formatter?.cursorForLength(normalizedLength, masked.length) ?: masked.length
        return TextFieldValue(masked, TextRange(cursorPos))
    }

    private fun desiredSelectionFor(
        formatter: MaskFormatter,
        selection: TextRange,
        textLength: Int
    ): Int {
        val desired = selection.start.coerceIn(0, textLength)
        val slots = formatter.validPositions
        if (slots.isEmpty()) {
            return desired
        }
        val trailing = ((formatter.lastValidPosition() ?: -1) + 1).coerceAtMost(textLength)
        if (desired >= trailing) {
            return trailing
        }
        if (slots.contains(desired)) {
            return desired
        }
        val nearest = formatter.nearestValidPosition(desired)
        return if (nearest == slots.last() && desired > nearest && trailing > nearest) {
            trailing
        } else {
            nearest.coerceIn(0, textLength)
        }
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
