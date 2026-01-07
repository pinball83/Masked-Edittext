package com.github.pinball83.maskededittext

/**
 * Represents events that can trigger state transitions
 */
enum class InputEvent {
    /**
     * User types a character
     */
    CHARACTER_TYPED,
    
    /**
     * User deletes a character
     */
    CHARACTER_DELETED,
    
    /**
     * User pastes text
     */
    TEXT_PASTED,
    
    /**
     * Input field gains focus
     */
    FOCUS_GAINED,
    
    /**
     * Input field loses focus
     */
    FOCUS_LOST,
    
    /**
     * Text is set programmatically
     */
    TEXT_SET,
    
    /**
     * Input is cleared
     */
    INPUT_CLEARED,
    
    /**
     * Validation is triggered
     */
    VALIDATE
}