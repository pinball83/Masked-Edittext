package com.github.pinball83.maskededittext


/**
 * State machine for managing masked input states and transitions
 */
class InputStateMachine(
    private val maskedEditText: MaskedEditText?,
    private val stateListener: InputStateListener?
) {

    interface InputStateListener {
        fun onStateChanged(oldState: InputState, newState: InputState, event: InputEvent)
    }

    interface MaskEvaluator {
        fun currentUnmasked(): String
        fun isComplete(unmasked: String): Boolean?
        fun isValid(unmasked: String): Boolean?
    }

    private var currentState: InputState = InputState.EMPTY
    private val transitions: MutableMap<StateTransitionKey, InputState> = HashMap()
    private var maskEvaluator: MaskEvaluator? = null
    private var lastEvent: InputEvent? = null
    private var composing: Boolean = false

    init {
        initializeTransitions()
    }

    fun setMaskEvaluator(evaluator: MaskEvaluator?) {
        maskEvaluator = evaluator
    }

    private fun initializeTransitions() {
        // From EMPTY state
        transitions[StateTransitionKey(InputState.EMPTY, InputEvent.CHARACTER_TYPED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.EMPTY, InputEvent.TEXT_PASTED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.EMPTY, InputEvent.TEXT_SET)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.EMPTY, InputEvent.FOCUS_GAINED)] = InputState.FOCUSED
        transitions[StateTransitionKey(InputState.EMPTY, InputEvent.FOCUS_LOST)] = InputState.UNFOCUSED

        // From PARTIAL state
        transitions[StateTransitionKey(InputState.PARTIAL, InputEvent.CHARACTER_TYPED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.PARTIAL, InputEvent.CHARACTER_DELETED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.PARTIAL, InputEvent.TEXT_PASTED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.PARTIAL, InputEvent.INPUT_CLEARED)] = InputState.EMPTY
        transitions[StateTransitionKey(InputState.PARTIAL, InputEvent.FOCUS_GAINED)] = InputState.FOCUSED
        transitions[StateTransitionKey(InputState.PARTIAL, InputEvent.FOCUS_LOST)] = InputState.UNFOCUSED

        // From COMPLETE state
        transitions[StateTransitionKey(InputState.COMPLETE, InputEvent.CHARACTER_DELETED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.COMPLETE, InputEvent.INPUT_CLEARED)] = InputState.EMPTY
        transitions[StateTransitionKey(InputState.COMPLETE, InputEvent.FOCUS_GAINED)] = InputState.FOCUSED
        transitions[StateTransitionKey(InputState.COMPLETE, InputEvent.FOCUS_LOST)] = InputState.UNFOCUSED

        // From INVALID state
        transitions[StateTransitionKey(InputState.INVALID, InputEvent.CHARACTER_TYPED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.INVALID, InputEvent.CHARACTER_DELETED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.INVALID, InputEvent.INPUT_CLEARED)] = InputState.EMPTY
        transitions[StateTransitionKey(InputState.INVALID, InputEvent.TEXT_SET)] = InputState.PARTIAL

        // From FOCUSED state
        transitions[StateTransitionKey(InputState.FOCUSED, InputEvent.CHARACTER_TYPED)] = InputState.PARTIAL
        transitions[StateTransitionKey(InputState.FOCUSED, InputEvent.FOCUS_LOST)] = InputState.UNFOCUSED

        // From UNFOCUSED state
        transitions[StateTransitionKey(InputState.UNFOCUSED, InputEvent.FOCUS_GAINED)] = InputState.FOCUSED
    }

    fun processEvent(event: InputEvent) {
        lastEvent = event
        val oldState = currentState
        val key = StateTransitionKey(currentState, event)
        var newState = transitions[key]

        if (newState != null) {
            currentState = newState
        } else {
            newState = handleSpecialTransition(currentState, event)
            if (newState != null) {
                currentState = newState
            }
        }

        // Validate current state based on input content
        currentState = validateCurrentState()

        if (oldState != currentState) {
            stateListener?.onStateChanged(oldState, currentState, event)
        }
    }

    fun getLastEvent(): InputEvent? = lastEvent

    /**
     * Computes a caret position policy based on the last input event and editable slot positions.
     * - If caret is already on a slot, keep it.
     * - If last event was delete, prefer previous slot.
     * - If last event was type, prefer next slot.
     * - Otherwise choose nearest slot (ties prefer forward).
     */
    fun caretPolicy(selStart: Int, textLength: Int, slots: List<Int>, firstSlot: Int?): Int {
        if (slots.isEmpty() || textLength <= 0) return selStart.coerceIn(0, textLength)
        val clampedSel = selStart.coerceIn(0, textLength)
        if (slots.contains(clampedSel)) return clampedSel

        return when (lastEvent) {
            InputEvent.CHARACTER_DELETED -> previousSlot(clampedSel, slots, firstSlot)
            InputEvent.CHARACTER_TYPED -> nextSlot(clampedSel, slots)
            else -> nearestSlot(clampedSel, slots)
        }.coerceIn(0, textLength)
    }

    /**
     * Same as caretPolicy, but uses provided event when non-null.
     */
    fun caretPolicyFor(
        event: InputEvent?,
        selStart: Int,
        textLength: Int,
        slots: List<Int>,
        firstSlot: Int?
    ): Int {
        if (slots.isEmpty() || textLength <= 0) return selStart.coerceIn(0, textLength)
        val clampedSel = selStart.coerceIn(0, textLength)
        if (slots.contains(clampedSel)) return clampedSel
        val ev = event ?: lastEvent
        return when (ev) {
            InputEvent.CHARACTER_DELETED -> previousSlot(clampedSel, slots, firstSlot)
            InputEvent.CHARACTER_TYPED -> nextSlot(clampedSel, slots)
            else -> nearestSlot(clampedSel, slots)
        }.coerceIn(0, textLength)
    }

    // --- Interaction helpers (optional, internal) ---
    fun setComposing(active: Boolean) {
        composing = active
    }

    fun isComposing(): Boolean = composing

    data class SelectionInfo(val kind: Kind, val start: Int, val end: Int) {
        enum class Kind {
            CollapsedAtSlot,
            CollapsedAtLiteral,
            Range,
            BeforeFirst,
            AfterLast
        }
    }

    fun computeSelectionInfo(
        selStart: Int,
        selEnd: Int,
        textLength: Int,
        slots: List<Int>
    ): SelectionInfo {
        val start = selStart.coerceIn(0, textLength)
        val end = selEnd.coerceIn(0, textLength)
        if (start != end) return SelectionInfo(SelectionInfo.Kind.Range, start, end)
        if (slots.isEmpty()) return SelectionInfo(SelectionInfo.Kind.CollapsedAtLiteral, start, end)
        val first = slots.first()
        val last = slots.last()
        val allowedTrailing = (last + 1).coerceAtMost(textLength)
        return when {
            start < first -> SelectionInfo(SelectionInfo.Kind.BeforeFirst, start, end)
            start > allowedTrailing -> SelectionInfo(SelectionInfo.Kind.AfterLast, start, end)
            slots.contains(start) -> SelectionInfo(SelectionInfo.Kind.CollapsedAtSlot, start, end)
            else -> SelectionInfo(SelectionInfo.Kind.CollapsedAtLiteral, start, end)
        }
    }

    fun isCaretAtLiteral(selStart: Int, slots: List<Int>): Boolean = !slots.contains(selStart)

    private fun previousSlot(pos: Int, slots: List<Int>, firstSlot: Int?): Int {
        val candidate = slots.lastOrNull { it < pos } ?: firstSlot ?: slots.first()
        return candidate
    }

    private fun nextSlot(pos: Int, slots: List<Int>): Int {
        val candidate = slots.firstOrNull { it > pos } ?: slots.last()
        return candidate
    }

    private fun nearestSlot(pos: Int, slots: List<Int>): Int {
        // Assumes slots sorted ascending
        if (pos <= slots.first()) return slots.first()
        if (pos >= slots.last()) return slots.last()
        var lower = slots.first()
        var upper = slots.last()
        for (s in slots) {
            if (s >= pos) { upper = s; break }
            lower = s
        }
        val distDown = pos - lower
        val distUp = upper - pos
        return if (distUp <= distDown) upper else lower
    }

    private fun handleSpecialTransition(currentState: InputState, event: InputEvent): InputState? {
        if (event == InputEvent.VALIDATE) {
            return validateCurrentState()
        }

        if (event == InputEvent.CHARACTER_TYPED && currentState == InputState.PARTIAL) {
            if (isInputComplete()) {
                return InputState.COMPLETE
            }
        }

        return null
    }

    private fun validateCurrentState(): InputState {
        val unmaskedText = getCurrentUnmaskedText()

        if (unmaskedText.isEmpty() || unmaskedText.trim().isEmpty()) {
            return InputState.EMPTY
        }

        if (isInputComplete()) {
            return InputState.COMPLETE
        }

        if (isInputValid()) {
            return InputState.PARTIAL
        }

        return InputState.INVALID
    }

    private fun getCurrentUnmaskedText(): String {
        maskEvaluator?.let { evaluator ->
            return evaluator.currentUnmasked()
        }
        return maskedEditText?.getUnmaskedText() ?: ""
    }

    private fun isInputComplete(): Boolean {
        val unmaskedText = getCurrentUnmaskedText()
        maskEvaluator?.let { evaluator ->
            evaluator.isComplete(unmaskedText)?.let { return it }
        }

        if (maskedEditText == null) {
            return false
        }

        if (unmaskedText.isEmpty()) {
            return false
        }

        return !unmaskedText.contains(" ") && unmaskedText.isNotEmpty()
    }

    private fun isInputValid(): Boolean {
        val unmaskedText = getCurrentUnmaskedText()
        maskEvaluator?.let { evaluator ->
            evaluator.isValid(unmaskedText)?.let { return it }
        }

        if (unmaskedText.isEmpty()) {
            return true
        }

        return DEFAULT_ALLOWED_REGEX.matches(unmaskedText)
    }

    fun getCurrentState(): InputState = currentState

    fun isComplete(): Boolean = currentState == InputState.COMPLETE

    fun isValid(): Boolean = currentState != InputState.INVALID

    fun isEmpty(): Boolean = currentState == InputState.EMPTY

    private data class StateTransitionKey(
        val fromState: InputState,
        val event: InputEvent
    )

    private companion object {
        val DEFAULT_ALLOWED_REGEX = "^[a-zA-Z0-9\\s]*$".toRegex()
    }
}
