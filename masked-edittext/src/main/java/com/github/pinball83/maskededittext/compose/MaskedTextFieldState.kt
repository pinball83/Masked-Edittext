package com.github.pinball83.maskededittext.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.pinball83.maskededittext.CursorPolicyController
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

    private val cursorController = CursorPolicyController()

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
        val fmt = formatter
        val previousRaw = rawUnmasked
        val previousCaret = textFieldValue.selection.start
        val previousLen = unmaskedValue.length

        var normalized = fmt?.let { formatter ->
            formatter.normalize(formatter.unmask(newValue.text))
        } ?: newValue.text
        var coercedBackspace = false
        if (fmt != null) {
            val slots = fmt.validPositions
            val isCollapsedSelection = newValue.selection.start == newValue.selection.end
            val caretMovedBackward = newValue.selection.start < previousCaret
            val maskedChanged = newValue.text != textFieldValue.text
            val prevMasked = textFieldValue.text
            val deletedIndex = previousCaret - 1
            val deletedOnSlot = deletedIndex >= 0 && slots.contains(deletedIndex)
            val deletedWasBlankSlot = deletedOnSlot && deletedIndex < prevMasked.length && prevMasked[deletedIndex] == ' '
            val deletedLiteral = previousCaret > 0 && (!deletedOnSlot || deletedWasBlankSlot)
            val prevSlotPosition = slots.lastOrNull { it < previousCaret }
            val slotIndex = prevSlotPosition?.let { slots.indexOf(it) } ?: -1
            val shouldCoerce = isCollapsedSelection && caretMovedBackward && maskedChanged && slotIndex >= 0 &&
                (deletedLiteral || normalized.length >= previousLen)

            if (shouldCoerce) {
                val base = StringBuilder(unmaskedValue)
                if (slotIndex in base.indices) {
                    base.deleteCharAt(slotIndex)
                }
                normalized = fmt.normalize(base)
                coercedBackspace = true
            }
        }
        val newRaw = computeRawFromNormalized(normalized)
        val textChanged = newRaw != previousRaw

        rawUnmasked = newRaw
        unmaskedValue = normalized

        val masked = fmt?.mask(normalized) ?: normalized
        val maskedLength = masked.length

        val event = if (textChanged) resolveEvent(previousRaw, newRaw) else null

        var caret = newValue.selection.start.coerceIn(0, maskedLength)

        if (fmt != null) {
            val slots = fmt.validPositions
            when {
                (event == InputEvent.CHARACTER_DELETED || coercedBackspace) && normalized.length <= previousLen -> {
                    caret = cursorController.focusTarget(fmt, normalized, maskedLength)
                }
                event == InputEvent.CHARACTER_TYPED || event == InputEvent.TEXT_PASTED -> {
                    caret = cursorController.focusTarget(fmt, normalized, maskedLength)
                }
                event == InputEvent.TEXT_SET && textChanged -> {
                    caret = cursorController.focusTarget(fmt, normalized, maskedLength)
                }
                !textChanged -> {
                    cursorController.selectionCorrection(
                        selStart = newValue.selection.start,
                        selEnd = newValue.selection.end,
                        textLength = maskedLength,
                        formatter = fmt,
                        stateMachine = stateMachine,
                        pendingEvent = null,
                        lastEvent = stateMachine.getLastEvent()
                    )?.let { caret = it }
                }
                else -> {
                    caret = fmt.cursorForLength(normalized.length, maskedLength)
                }
            }

            val trailing = ((slots.lastOrNull() ?: -1) + 1).coerceAtMost(maskedLength)
            caret = caret.coerceIn(0, trailing)

            if (event == null && newValue.selection.start == newValue.selection.end && caret < trailing && !slots.contains(caret)) {
                val adjusted = stateMachine.caretPolicyFor(
                    stateMachine.getLastEvent(),
                    caret,
                    maskedLength,
                    slots,
                    fmt.firstValidPosition()
                )
                caret = adjusted.coerceIn(0, trailing)
            }
        }

        textFieldValue = newValue.copy(text = masked, selection = TextRange(caret))

        event?.let { stateMachine.processEvent(it) }
    }

    fun updateValue(newValue: String) {
        val fmt = formatter
        val previousRaw = rawUnmasked
        val previousCaret = textFieldValue.selection.start
        val previousLen = unmaskedValue.length

        val normalized = formatNormalized(newValue)
        val newRaw = computeRawFromNormalized(normalized)
        val textChanged = newRaw != previousRaw

        rawUnmasked = newRaw
        unmaskedValue = normalized

        val masked = fmt?.mask(normalized) ?: normalized
        val maskedLength = masked.length

        var caret = textFieldValue.selection.start.coerceIn(0, maskedLength)

        if (fmt != null && textChanged) {
            caret = if (normalized.length < previousLen) {
                cursorController.focusTarget(fmt, normalized, maskedLength)
            } else {
                cursorController.focusTarget(fmt, normalized, maskedLength)
            }
        } else if (fmt == null && textChanged) {
            caret = maskedLength
        }

        textFieldValue = TextFieldValue(masked, TextRange(caret.coerceIn(0, maskedLength)))

        if (textChanged) {
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
