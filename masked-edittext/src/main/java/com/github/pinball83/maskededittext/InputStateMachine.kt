package com.github.pinball83.maskededittext

import android.text.TextUtils

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
    
    private var currentState: InputState = InputState.EMPTY
    private val transitions: MutableMap<StateTransitionKey, InputState> = HashMap()
    
    init {
        initializeTransitions()
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
            // Handle special cases or stay in current state
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
        // Handle validation events
        if (event == InputEvent.VALIDATE) {
            return validateCurrentState()
        }
        
        // Handle character typing that might complete the input
        if (event == InputEvent.CHARACTER_TYPED && currentState == InputState.PARTIAL) {
            if (isInputComplete()) {
                return InputState.COMPLETE
            }
        }
        
        return null
    }
    
    private fun validateCurrentState(): InputState {
        if (maskedEditText == null) {
            return currentState
        }
        
        val unmaskedText = maskedEditText.getUnmaskedText()
        
        // Check if input is empty
        if (TextUtils.isEmpty(unmaskedText) || unmaskedText.trim().isEmpty()) {
            return InputState.EMPTY
        }
        
        // Check if input is complete
        if (isInputComplete()) {
            return InputState.COMPLETE
        }
        
        // Check if input is valid so far
        if (isInputValid()) {
            return InputState.PARTIAL
        }
        
        return InputState.INVALID
    }
    
    private fun isInputComplete(): Boolean {
        if (maskedEditText == null) {
            return false
        }
        
        val unmaskedText = maskedEditText.getUnmaskedText()
        if (TextUtils.isEmpty(unmaskedText)) {
            return false
        }
        
        // Check if all required positions are filled
        // This is a simplified check - in a real implementation, you'd check against the mask
        return !unmaskedText.contains(" ") && unmaskedText.isNotEmpty()
    }
    
    private fun isInputValid(): Boolean {
        if (maskedEditText == null) {
            return true
        }
        
        val unmaskedText = maskedEditText.getUnmaskedText()
        if (TextUtils.isEmpty(unmaskedText)) {
            return true
        }
        
        // Basic validation - can be extended based on specific requirements
        return unmaskedText.matches("^[a-zA-Z0-9\\s]*$".toRegex())
    }
    
    fun getCurrentState(): InputState = currentState
    
    fun isComplete(): Boolean = currentState == InputState.COMPLETE
    
    fun isValid(): Boolean = currentState != InputState.INVALID
    
    fun isEmpty(): Boolean = currentState == InputState.EMPTY
    
    /**
     * Key class for state transition mapping
     */
    private data class StateTransitionKey(
        val fromState: InputState,
        val event: InputEvent
    )
}
