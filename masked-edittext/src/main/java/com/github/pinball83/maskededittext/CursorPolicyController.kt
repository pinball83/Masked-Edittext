package com.github.pinball83.maskededittext

internal class CursorPolicyController {

    fun focusTarget(formatter: MaskFormatter, unmasked: String, maskedLength: Int): Int {
        val normalizedLen = formatter.normalize(unmasked).length
        val slots = formatter.validPositions
        val target = when {
            normalizedLen <= 0 -> formatter.firstValidPosition() ?: 0
            normalizedLen >= slots.size -> ((formatter.lastValidPosition() ?: -1) + 1).coerceAtMost(maskedLength)
            else -> slots[normalizedLen]
        }
        return target.coerceIn(0, maskedLength)
    }

    fun selectionCorrection(
        selStart: Int,
        selEnd: Int,
        textLength: Int,
        formatter: MaskFormatter,
        stateMachine: InputStateMachine,
        pendingEvent: InputEvent?,
        lastEvent: InputEvent?
    ): Int? {
        val slots = formatter.validPositions
        val firstSlot = formatter.firstValidPosition()
        val info = stateMachine.computeSelectionInfo(selStart, selEnd, textLength, slots)
        return when (info.kind) {
            InputStateMachine.SelectionInfo.Kind.BeforeFirst -> (firstSlot ?: 0).coerceIn(0, textLength)
            InputStateMachine.SelectionInfo.Kind.AfterLast -> {
                val last = formatter.lastValidPosition() ?: -1
                val trailing = (last + 1).coerceAtMost(textLength)
                trailing
            }
            InputStateMachine.SelectionInfo.Kind.CollapsedAtLiteral -> {
                val ev = pendingEvent ?: lastEvent
                stateMachine.caretPolicyFor(ev, selStart, textLength, slots, firstSlot)
            }
            InputStateMachine.SelectionInfo.Kind.CollapsedAtSlot -> null
            else -> null
        }
    }

    fun afterTypedAdvance(
        selStart: Int,
        textLength: Int,
        formatter: MaskFormatter,
        stateMachine: InputStateMachine
    ): Int? {
        val slots = formatter.validPositions
        val firstSlot = formatter.firstValidPosition()
        val desired = stateMachine.caretPolicyFor(
            InputEvent.CHARACTER_TYPED,
            selStart,
            textLength,
            slots,
            firstSlot
        )
        return if (desired != selStart) desired.coerceIn(0, textLength) else null
    }

    fun beforeBackspaceReposition(
        caret: Int,
        textLength: Int,
        formatter: MaskFormatter
    ): Int {
        val slots = formatter.validPositions
        val firstSlot = formatter.firstValidPosition()
        val prevSlot = slots.lastOrNull { it < caret } ?: (firstSlot ?: 0)
        return (prevSlot + 1).coerceIn(0, textLength)
    }
}
