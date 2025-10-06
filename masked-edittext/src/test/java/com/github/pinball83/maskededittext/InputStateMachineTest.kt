package com.github.pinball83.maskededittext;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InputStateMachine
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class InputStateMachineTest {

    private Context context;
    private MaskedEditText maskedEditText;
    private InputStateMachine stateMachine;

    @Mock
    private InputStateMachine.InputStateListener mockStateListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.application;
        maskedEditText = new MaskedEditText.Builder(context)
                .mask("8 (***) *** **-**")
                .notMaskedSymbol("*")
                .build();
        stateMachine = new InputStateMachine(maskedEditText, mockStateListener);
    }

    @Test
    public void testInitialState() {
        assertEquals(InputState.EMPTY, stateMachine.getCurrentState());
        assertTrue(stateMachine.isEmpty());
        assertFalse(stateMachine.isComplete());
        assertTrue(stateMachine.isValid());
    }

    @Test
    public void testCharacterTypedFromEmpty() {
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED);
        
        // Should transition to PARTIAL state
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState());
        assertFalse(stateMachine.isEmpty());
        assertFalse(stateMachine.isComplete());
        
        // Verify state change notification
        verify(mockStateListener).onStateChanged(InputState.EMPTY, InputState.PARTIAL, InputEvent.CHARACTER_TYPED);
    }

    @Test
    public void testFocusGainedFromEmpty() {
        stateMachine.processEvent(InputEvent.FOCUS_GAINED);
        
        assertEquals(InputState.FOCUSED, stateMachine.getCurrentState());
        verify(mockStateListener).onStateChanged(InputState.EMPTY, InputState.FOCUSED, InputEvent.FOCUS_GAINED);
    }

    @Test
    public void testFocusLostFromFocused() {
        stateMachine.processEvent(InputEvent.FOCUS_GAINED);
        reset(mockStateListener); // Clear previous interactions
        
        stateMachine.processEvent(InputEvent.FOCUS_LOST);
        
        assertEquals(InputState.UNFOCUSED, stateMachine.getCurrentState());
        verify(mockStateListener).onStateChanged(InputState.FOCUSED, InputState.UNFOCUSED, InputEvent.FOCUS_LOST);
    }

    @Test
    public void testInputClearedFromPartial() {
        // First, move to partial state
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED);
        reset(mockStateListener);
        
        stateMachine.processEvent(InputEvent.INPUT_CLEARED);
        
        assertEquals(InputState.EMPTY, stateMachine.getCurrentState());
        assertTrue(stateMachine.isEmpty());
        verify(mockStateListener).onStateChanged(InputState.PARTIAL, InputState.EMPTY, InputEvent.INPUT_CLEARED);
    }

    @Test
    public void testCompleteInput() {
        // Set complete input
        maskedEditText.setMaskedText("1234567890");
        stateMachine.processEvent(InputEvent.TEXT_SET);
        
        // Should be in COMPLETE state
        assertEquals(InputState.COMPLETE, stateMachine.getCurrentState());
        assertTrue(stateMachine.isComplete());
        assertFalse(stateMachine.isEmpty());
    }

    @Test
    public void testCharacterDeletedFromComplete() {
        // First, set complete input
        maskedEditText.setMaskedText("1234567890");
        stateMachine.processEvent(InputEvent.TEXT_SET);
        reset(mockStateListener);
        
        stateMachine.processEvent(InputEvent.CHARACTER_DELETED);
        
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState());
        assertFalse(stateMachine.isComplete());
        verify(mockStateListener).onStateChanged(InputState.COMPLETE, InputState.PARTIAL, InputEvent.CHARACTER_DELETED);
    }

    @Test
    public void testValidationEvent() {
        maskedEditText.setMaskedText("12345");
        stateMachine.processEvent(InputEvent.VALIDATE);
        
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState());
        assertTrue(stateMachine.isValid());
    }

    @Test
    public void testTextPastedEvent() {
        stateMachine.processEvent(InputEvent.TEXT_PASTED);
        
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState());
        verify(mockStateListener).onStateChanged(InputState.EMPTY, InputState.PARTIAL, InputEvent.TEXT_PASTED);
    }

    @Test
    public void testMultipleStateTransitions() {
        // Empty -> Focused -> Partial -> Complete -> Partial -> Empty
        
        stateMachine.processEvent(InputEvent.FOCUS_GAINED);
        assertEquals(InputState.FOCUSED, stateMachine.getCurrentState());
        
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED);
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState());
        
        maskedEditText.setMaskedText("1234567890");
        stateMachine.processEvent(InputEvent.TEXT_SET);
        assertEquals(InputState.COMPLETE, stateMachine.getCurrentState());
        
        stateMachine.processEvent(InputEvent.CHARACTER_DELETED);
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState());
        
        stateMachine.processEvent(InputEvent.INPUT_CLEARED);
        assertEquals(InputState.EMPTY, stateMachine.getCurrentState());
        
        // Verify all state changes were notified
        verify(mockStateListener, times(5)).onStateChanged(any(InputState.class), any(InputState.class), any(InputEvent.class));
    }

    @Test
    public void testInvalidTransition() {
        // Try an invalid transition - should stay in current state
        InputState initialState = stateMachine.getCurrentState();
        
        // There's no direct transition from EMPTY to COMPLETE
        stateMachine.processEvent(InputEvent.CHARACTER_DELETED);
        
        assertEquals(initialState, stateMachine.getCurrentState());
        
        // Should not notify of state change since state didn't change
        verify(mockStateListener, never()).onStateChanged(any(InputState.class), any(InputState.class), any(InputEvent.class));
    }

    @Test
    public void testStateListenerArguments() {
        ArgumentCaptor<InputState> oldStateCaptor = ArgumentCaptor.forClass(InputState.class);
        ArgumentCaptor<InputState> newStateCaptor = ArgumentCaptor.forClass(InputState.class);
        ArgumentCaptor<InputEvent> eventCaptor = ArgumentCaptor.forClass(InputEvent.class);
        
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED);
        
        verify(mockStateListener).onStateChanged(oldStateCaptor.capture(), newStateCaptor.capture(), eventCaptor.capture());
        
        assertEquals(InputState.EMPTY, oldStateCaptor.getValue());
        assertEquals(InputState.PARTIAL, newStateCaptor.getValue());
        assertEquals(InputEvent.CHARACTER_TYPED, eventCaptor.getValue());
    }
}