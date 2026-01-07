package com.github.pinball83.maskededittext

/**
 * Represents the different states of masked input
 */
enum class InputState {
    /**
     * Input field is empty or contains only mask characters
     */
    EMPTY,
    
    /**
     * Input field contains partial valid input
     */
    PARTIAL,
    
    /**
     * Input field is completely filled with valid input
     */
    COMPLETE,
    
    /**
     * Input field contains invalid characters or format
     */
    INVALID,
    
    /**
     * Input field is in focus and ready for input
     */
    FOCUSED,
    
    /**
     * Input field has lost focus
     */
    UNFOCUSED
}