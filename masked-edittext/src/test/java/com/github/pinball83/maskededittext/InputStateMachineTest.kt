package com.github.pinball83.maskededittext

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InputStateMachineTest {

    private lateinit var stateMachine: InputStateMachine
    private lateinit var formatter: MaskFormatter
    private var currentUnmasked: String = ""

    @Before
    fun setUp() {
        formatter = MaskFormatter(
            maskPattern = "**** **** **** ****",
            placeholder = '*'
        )
        stateMachine = InputStateMachine(maskedEditText = null, stateListener = null)
        stateMachine.setMaskEvaluator(object : InputStateMachine.MaskEvaluator {
            override fun currentUnmasked(): String = currentUnmasked
            override fun isComplete(unmasked: String): Boolean? = formatter.isComplete(unmasked)
            override fun isValid(unmasked: String): Boolean? = null
        })
    }

    @Test
    fun `typing transitions to partial and complete`() {
        currentUnmasked = formatter.unmask("                ")
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED)
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState())

        currentUnmasked = formatter.unmask(formatter.mask("1234567890123456"))
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED)
        assertEquals(InputState.COMPLETE, stateMachine.getCurrentState())
    }

    @Test
    fun `deletion returns to partial`() {
        currentUnmasked = formatter.unmask(formatter.mask("1234567890123456"))
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED)
        assertEquals(InputState.COMPLETE, stateMachine.getCurrentState())

        currentUnmasked = formatter.unmask(formatter.mask("12345678"))
        stateMachine.processEvent(InputEvent.CHARACTER_DELETED)
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState())
    }

    @Test
    fun `validate flags invalid characters`() {
        currentUnmasked = "1234$678"
        stateMachine.processEvent(InputEvent.VALIDATE)
        assertEquals(InputState.INVALID, stateMachine.getCurrentState())
    }

    @Test
    fun `input cleared resets state`() {
        currentUnmasked = formatter.unmask(formatter.mask("1234"))
        stateMachine.processEvent(InputEvent.CHARACTER_TYPED)
        assertEquals(InputState.PARTIAL, stateMachine.getCurrentState())

        currentUnmasked = formatter.unmask("")
        stateMachine.processEvent(InputEvent.INPUT_CLEARED)
        assertEquals(InputState.EMPTY, stateMachine.getCurrentState())
    }
}
