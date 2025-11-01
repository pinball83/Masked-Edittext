package com.github.pinball83.maskededittext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InputInteractionHelpersTest {

    private lateinit var phoneFormatter: MaskFormatter
    private lateinit var cardFormatter: MaskFormatter
    private lateinit var machine: InputStateMachine

    @Before
    fun setUp() {
        phoneFormatter = MaskFormatter(
            maskPattern = "8 (***) *** **-**",
            placeholder = '*'
        )
        cardFormatter = MaskFormatter(
            maskPattern = "**** **** **** ****",
            placeholder = '*'
        )
        machine = InputStateMachine(maskedEditText = null, stateListener = null)
        machine.setMaskEvaluator(object : InputStateMachine.MaskEvaluator {
            override fun currentUnmasked(): String = ""
            override fun isComplete(unmasked: String): Boolean? = null
            override fun isValid(unmasked: String): Boolean? = null
        })
    }

    @Test
    fun `selection info before-first and after-last`() {
        val slots = phoneFormatter.validPositions
        val first = phoneFormatter.firstValidPosition() ?: 0
        val last = phoneFormatter.lastValidPosition() ?: first
        val textLength = phoneFormatter.mask("").length
        val trailing = (last + 1).coerceAtMost(textLength)

        // Before first slot
        val before = machine.computeSelectionInfo(first - 1, first - 1, textLength, slots)
        assertEquals(InputStateMachine.SelectionInfo.Kind.BeforeFirst, before.kind)

        // After last (at trailing is also considered AfterLast)
        val after = machine.computeSelectionInfo(trailing, trailing, textLength, slots)
        assertEquals(InputStateMachine.SelectionInfo.Kind.AfterLast, after.kind)
    }

    @Test
    fun `selection info collapsed at literal`() {
        val slots = phoneFormatter.validPositions
        val textLength = phoneFormatter.mask("").length
        // pick a known literal: index of ')' in the mask
        val literalIdx = "8 (***) *** **-**".indexOf(')')
        require(literalIdx >= 0)
        val info = machine.computeSelectionInfo(literalIdx, literalIdx, textLength, slots)
        assertEquals(InputStateMachine.SelectionInfo.Kind.CollapsedAtLiteral, info.kind)
        assertTrue(machine.isCaretAtLiteral(literalIdx, slots))
    }

    @Test
    fun `caret policy advances to trailing on last typed`() {
        val slots = cardFormatter.validPositions
        val first = cardFormatter.firstValidPosition() ?: 0
        val last = cardFormatter.lastValidPosition() ?: first
        val textLength = cardFormatter.mask("").length
        // Simulate typing event; caret positioned at last slot
        val posAtLast = last
        val desired = machine.caretPolicyFor(
            event = InputEvent.CHARACTER_TYPED,
            selStart = posAtLast,
            textLength = textLength,
            slots = slots,
            firstSlot = first
        )
        val trailing = (last + 1).coerceAtMost(textLength)
        assertEquals(trailing, desired)
    }

    @Test
    fun `composition flags toggle`() {
        assertFalse(machine.isComposing())
        machine.setComposing(true)
        assertTrue(machine.isComposing())
        machine.setComposing(false)
        assertFalse(machine.isComposing())
    }
}

