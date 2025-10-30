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
