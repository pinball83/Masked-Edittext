package com.github.pinball83.maskededittext

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.drawable.DrawableCompat
import com.thrd.maskededittext.R

/**
 * Kotlin implementation of the legacy MaskedEditText widget with shared mask logic.
 */
class MaskedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr), View.OnTouchListener,
    View.OnFocusChangeListener {

    private var mask: String = ""
    private var notMaskedSymbol: String = "*"
    private var format: String? = null
    private var required: Boolean = false
    private var maskIconCallback: MaskIconCallback? = null
    private var iconCallback: IconCallback? = null
    private var maskIcon: Drawable? = null

    private var maskFormatter: MaskFormatter? = null
    private var userFocusChangeListener: OnFocusChangeListener? = null

    private var stateChangeListener: InputStateMachine.InputStateListener? = null
    private var stateMachine: InputStateMachine? = null
    private var pendingInputEvent: InputEvent? = null
    private var adjustingSelection: Boolean = false
    private var suppressFilter: Boolean = false
    private var pendingSelection: Int? = null
    private var pendingText: String? = null
    private var adjustingText: Boolean = false
    private var lastEvent: InputEvent? = null
    private var deferFocusSelection: Boolean = false
    private val cursorController = CursorPolicyController()

    private fun MaskFormatter.cursorForNormalizedLength(
        normalizedLength: Int,
        textLength: Int
    ): Int {
        return when {
            normalizedLength <= 0 -> firstValidPosition() ?: 0
            normalizedLength >= validPositions.size -> {
                (lastValidPosition()?.plus(1))?.coerceAtMost(textLength) ?: textLength
            }

            else -> cursorPositionFor(normalizedLength)
        }.coerceIn(0, textLength)
    }

    private fun setSelectionSafely(position: Int) {
        val target = position.coerceIn(0, text?.length ?: 0)
        if (!adjustingSelection) {
            adjustingSelection = true
            setSelection(target)
            adjustingSelection = false
        } else {
            setSelection(target)
        }
    }

    private fun adjustCaretForwardIfOnLiteralTyped() {
        val fmt = maskFormatter ?: return
        if (lastEvent != InputEvent.CHARACTER_TYPED) return
        if (selectionStart != selectionEnd) return
        val slots = fmt.validPositions
        val textLen = text?.length ?: 0
        val caret = selectionStart
        if (!slots.contains(caret)) {
            val desired = stateMachine?.caretPolicyFor(
                InputEvent.CHARACTER_TYPED,
                caret,
                textLen,
                slots,
                fmt.firstValidPosition()
            ) ?: caret
            if (desired != caret) {
                adjustingSelection = true
                setSelection(desired.coerceIn(0, textLen))
                adjustingSelection = false
            }
        }
    }

    init {
        initFromAttributes(attrs, defStyleAttr)
        setupStateMachine()
        initializeMask()
    }

    private fun initFromAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(attrs, R.styleable.MaskedEditText, defStyleAttr, 0).apply {
            try {
                mask = getString(R.styleable.MaskedEditText_mask) ?: ""
                notMaskedSymbol = getString(R.styleable.MaskedEditText_notMaskedSymbol) ?: "*"
                format = getString(R.styleable.MaskedEditText_format)
                required = getBoolean(R.styleable.MaskedEditText_required, false)

                val iconResId = getResourceId(R.styleable.MaskedEditText_maskIcon, -1)
                if (iconResId != -1) {
                    context.getDrawable(iconResId)?.let {
                        maskIcon = DrawableCompat.wrap(it)
                    }
                }
            } finally {
                recycle()
            }
        }
    }

    private fun setupStateMachine() {
        stateMachine = InputStateMachine(
            maskedEditText = this,
            stateListener = object : InputStateMachine.InputStateListener {
                override fun onStateChanged(
                    oldState: InputState,
                    newState: InputState,
                    event: InputEvent
                ) {
                    lastEvent = event
                    stateChangeListener?.onStateChanged(oldState, newState, event)
                }
            }
        )
        updateStateEvaluator()
    }

    fun setStateChangeListener(listener: InputStateMachine.InputStateListener?) {
        stateChangeListener = listener
    }

    fun getCurrentInputState(): InputState? = stateMachine?.getCurrentState()

    fun isInputComplete(): Boolean = stateMachine?.isComplete() ?: false

    fun isInputValid(): Boolean = stateMachine?.isValid() ?: true

    fun isInputEmpty(): Boolean = stateMachine?.isEmpty() ?: true

    private fun initializeMask() {
        val placeholder = notMaskedSymbol.firstOrNull()
        maskFormatter = if (mask.isNotEmpty() && placeholder != null) {
            MaskFormatter(mask, placeholder, format)
        } else {
            null
        }

        val formatter = maskFormatter
        if (formatter != null) {
            // Auto mode: enable deferred focus selection if the first slot is not at index 0
            val firstSlotAuto = (formatter.firstValidPosition() ?: 0)
            deferFocusSelection = firstSlotAuto > 0
            filters = arrayOf(MaskedInputFilter())
            setOnTouchListener(this)
            super.setOnFocusChangeListener(this)

            val currentMasked = text?.toString().orEmpty()
            val reApplied = formatter.mask(getUnmaskedText(currentMasked))
            if (currentMasked != reApplied) {
                setText(reApplied)
            }
            val normalizedLength = formatter.normalize(getUnmaskedText()).length
            val cursorPosition =
                formatter.cursorForNormalizedLength(normalizedLength, reApplied.length)
            setSelectionSafely(cursorPosition)
        } else {
            filters = arrayOf()
        }

        updateStateEvaluator()
    }

    private fun updateStateEvaluator() {
        val formatter = maskFormatter
        stateMachine?.setMaskEvaluator(object : InputStateMachine.MaskEvaluator {
            override fun currentUnmasked(): String = getUnmaskedText()

            override fun isComplete(unmasked: String): Boolean? = formatter?.isComplete(unmasked)

            override fun isValid(unmasked: String): Boolean? = null
        })
    }

    // applyMask/applyMaskToText were unused; mask application is done inline

    fun getUnmaskedText(maskedText: String = text?.toString().orEmpty()): String {
        val formatter = maskFormatter ?: return maskedText
        return formatter.unmask(maskedText)
    }

    fun getFormattedText(): String {
        val formatter = maskFormatter ?: return getUnmaskedText().trim()
        return formatter.formatOutput(getUnmaskedText())
    }

    // Cursor policy handled by selection change logic

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP && maskIcon != null) {
            val iconStart = width - paddingRight - maskIcon!!.intrinsicWidth
            if (event.x >= iconStart) {
                val unmaskedText = getUnmaskedText()
                maskIconCallback?.onMaskIconClick(v as MaskedEditText, unmaskedText)
                iconCallback?.onIconClick(unmaskedText.trim())
                return true
            }
        }
        return false
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            stateMachine?.processEvent(InputEvent.FOCUS_GAINED)
            val formatter = maskFormatter
            if (formatter != null) {
                val unmasked = getUnmaskedText()
                val normalized = formatter.normalize(unmasked)
                val currentMaskedLen = text?.length ?: 0
                val target = formatter.cursorForNormalizedLength(normalized.length, currentMaskedLen)
                setSelectionSafely(target)
                if (deferFocusSelection) {
                    // Reassert on next frame to outrun IME-set selection on some keyboards
                    post { setSelectionSafely(target) }
                }
            } else if (text.isNullOrEmpty()) {
                setSelectionSafely(0)
            }
        } else {
            stateMachine?.processEvent(InputEvent.FOCUS_LOST)
        }

        userFocusChangeListener?.onFocusChange(v, hasFocus)
    }

    /**
     * When enabled, reasserts caret position once more after focus gain on the next frame.
     * Useful for IMEs that set selection after we do on focus.
     */
    fun setDeferFocusSelection(enabled: Boolean) {
        deferFocusSelection = enabled
    }

    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        userFocusChangeListener = l
    }

    fun setMask(mask: String) {
        this.mask = mask
        initializeMask()
    }

    fun getMask(): String = mask

    fun setNotMaskedSymbol(notMaskedSymbol: String) {
        this.notMaskedSymbol = notMaskedSymbol
        initializeMask()
    }

    fun getNotMaskedSymbol(): String = notMaskedSymbol

    fun setFormat(format: String?) {
        this.format = format
        initializeMask()
    }

    fun getFormat(): String? = format

    fun setRequired(required: Boolean) {
        this.required = required
    }

    fun isRequired(): Boolean = required

    fun setMaskIcon(@DrawableRes iconRes: Int) {
        context.getDrawable(iconRes)?.let {
            maskIcon = DrawableCompat.wrap(it)
            updateIconVisibility()
        }
    }

    fun setMaskIcon(icon: Drawable?) {
        maskIcon = icon?.let { DrawableCompat.wrap(it) }
        updateIconVisibility()
    }

    private fun updateIconVisibility() {
        val drawables = compoundDrawables
        setCompoundDrawablesWithIntrinsicBounds(
            drawables[0], drawables[1], maskIcon, drawables[3]
        )
    }

    fun setMaskIconCallback(callback: MaskIconCallback?) {
        maskIconCallback = callback
    }

    fun setIconCallback(callback: IconCallback?) {
        iconCallback = callback
    }

    fun setMaskedText(unmaskedText: String) {
        val formatter = maskFormatter
        if (formatter == null || mask.isEmpty()) {
            setText(unmaskedText)
            setSelection(unmaskedText.length)
        } else {
            val maskedText = formatter.mask(unmaskedText)
            setText(maskedText)
            val normalizedLength = formatter.normalize(unmaskedText).length
            val cursorPosition =
                formatter.cursorForNormalizedLength(normalizedLength, maskedText.length)
            setSelection(cursorPosition)
        }
        stateMachine?.processEvent(InputEvent.TEXT_SET)
    }

    fun clearMaskedText() {
        val formatter = maskFormatter
        if (formatter == null) {
            setText("")
            stateMachine?.processEvent(InputEvent.INPUT_CLEARED)
            return
        }
        val emptyMasked = formatter.mask("")
        setText(emptyMasked)
        setSelection(formatter.firstValidPosition() ?: 0)
        stateMachine?.processEvent(InputEvent.INPUT_CLEARED)
    }

    private inner class MaskedInputFilter : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            if (suppressFilter) {
                return null
            }

            val formatter = maskFormatter ?: return null
            if (source == null) return null

            val destMasked = dest?.toString() ?: ""
            val destUnmasked = formatter.unmask(destMasked)
            val src = source.subSequence(start, end).toString()

            val cleanedSrc = when {
                src.isEmpty() -> ""
                src.length > 1 -> formatter.normalize(formatter.unmask(src))
                else -> formatter.normalize(src)
            }

            pendingInputEvent = when {
                src.isEmpty() -> InputEvent.CHARACTER_DELETED
                src.length > 1 -> InputEvent.TEXT_PASTED
                else -> InputEvent.CHARACTER_TYPED
            }
            // Record intent early so selection correction can bias before state updates
            lastEvent = pendingInputEvent

            val slots = formatter.validPositions

            fun slotIndexForPosition(pos: Int): Int {
                if (slots.isEmpty()) return pos.coerceAtLeast(0)
                val idx = slots.indexOfFirst { it >= pos }
                return if (idx == -1) slots.size else idx
            }

            val startSlot = slotIndexForPosition(dstart)
            val endSlot = slotIndexForPosition(dend)

            var targetSlotIndex = startSlot
            val newUnmasked = if (src.isEmpty()) {
                var from = startSlot.coerceAtMost(destUnmasked.length)
                var to = endSlot.coerceIn(from, destUnmasked.length)

                if (from == to) {
                    if (dstart > 0) {
                        val targetSlot = (startSlot - 1).coerceAtLeast(0)
                        from = targetSlot.coerceAtMost(destUnmasked.length)
                        to = (targetSlot + 1).coerceIn(from, destUnmasked.length)
                        targetSlotIndex = targetSlot
                    } else if (endSlot < destUnmasked.length) {
                        from = endSlot.coerceAtMost(destUnmasked.length)
                        to = (endSlot + 1).coerceIn(from, destUnmasked.length)
                        targetSlotIndex = endSlot
                    } else {
                        targetSlotIndex = from
                    }
                } else {
                    targetSlotIndex = from
                }
                val trimmed = destUnmasked.removeRange(from, to)
                formatter.normalize(trimmed)
            } else {
                val inserted =
                    destUnmasked.substring(0, startSlot) + cleanedSrc + destUnmasked.substring(
                        endSlot
                    )
                targetSlotIndex = (startSlot + cleanedSrc.length)
                formatter.normalize(inserted)
            }

            val normalizedUnmasked = newUnmasked
            val maskedNew = formatter.mask(normalizedUnmasked)
            val maskedLength = maskedNew.length

            var clampedSlotIndex = targetSlotIndex.coerceIn(0, normalizedUnmasked.length)
            if (src.isEmpty() && targetSlotIndex >= startSlot && startSlot < slots.size) {
                clampedSlotIndex = (clampedSlotIndex + 1).coerceIn(0, normalizedUnmasked.length)
            }
            val targetSelection =
                formatter.cursorForNormalizedLength(clampedSlotIndex, maskedLength)

            // If nothing effectively changes, let the system handle it
            val naiveProposed = destMasked.substring(0, dstart) + src + destMasked.substring(dend)
            pendingSelection = targetSelection
            if (maskedNew != naiveProposed) {
                pendingText = maskedNew
            } else {
                pendingText = null
            }

            return null
        }
    }


    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        super.onTextChanged(text, start, before, count)

        if (adjustingText) {
            adjustingText = false
            return
        }

        val formatter = maskFormatter
        if (formatter != null && text != null) {
            val current = text.toString()

            pendingText?.let { desired ->
                pendingText = null
                val target = (pendingSelection ?: desired.length).coerceIn(0, desired.length)
                pendingSelection = null

                if (current != desired) {
                    suppressFilter = true
                    adjustingText = true
                    setText(desired)
                    adjustingText = false
                    suppressFilter = false
                }
                if (!adjustingSelection) {
                    adjustingSelection = true
                    setSelection(target)
                    adjustingSelection = false
                } else {
                    setSelection(target)
                }
                return
            }

            val unmasked = formatter.unmask(current)
            val remasked = formatter.mask(unmasked)

            // Re-apply mask if needed to keep template stable
            if (current != remasked) {
                val normalizedLength = formatter.normalize(unmasked).length
                pendingText = remasked
                pendingSelection =
                    formatter.cursorForNormalizedLength(normalizedLength, remasked.length)

                suppressFilter = true
                adjustingText = true
                setText(remasked)
                adjustingText = false
                suppressFilter = false

                return
            }
        }

        val event = pendingInputEvent
        if (event != null) {
            lastEvent = event
            stateMachine?.processEvent(event)
            pendingInputEvent = null
        } else if (before != 0 || count != 0) {
            stateMachine?.processEvent(InputEvent.VALIDATE)
        }

        var movedByFilter = false
        pendingSelection?.let { desired ->
            val target = desired.coerceIn(0, text?.length ?: 0)
            if (!adjustingSelection) {
                adjustingSelection = true
                setSelection(target)
                adjustingSelection = false
            }
            pendingSelection = null
            movedByFilter = true
        }

        // Centralized caret advance after any text insertion (handles multi-char commits and IME orderings)
        run {
            val fmt = maskFormatter
            val sm = stateMachine
            val textLen = text?.length ?: 0
            if (fmt != null && sm != null && selectionStart == selectionEnd) {
                // If we're already at the focus target (next empty slot or trailing), do nothing
                val focusTarget = cursorController.focusTarget(fmt, getUnmaskedText(), textLen)
                if (selectionStart != focusTarget) {
                    cursorController.afterTypedAdvance(selectionStart, textLen, fmt, sm)?.let { desired ->
                        setSelectionSafely(desired)
                    }
                }
            }
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (adjustingSelection) {
            super.onSelectionChanged(selStart, selEnd)
            return
        }

        val formatter = maskFormatter
        if (formatter != null) {
            val textLength = text?.length ?: 0
            val sm = stateMachine
            if (sm != null) {
                val target = cursorController.selectionCorrection(
                    selStart = selStart,
                    selEnd = selEnd,
                    textLength = textLength,
                    formatter = formatter,
                    stateMachine = sm,
                    pendingEvent = pendingInputEvent,
                    lastEvent = lastEvent
                )
                if (target != null && target != selStart) {
                    setSelectionSafely(target)
                    super.onSelectionChanged(target, target)
                    return
                }
            }
        }

        super.onSelectionChanged(selStart, selEnd)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? =
        super.onCreateInputConnection(outAttrs)?.let { base ->
            object : InputConnectionWrapper(base, true) {
                // Track composition to help downstream caret policy when needed
                override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    stateMachine?.setComposing(true)
                    return super.setComposingText(text, newCursorPosition)
                }

                override fun finishComposingText(): Boolean {
                    stateMachine?.setComposing(false)
                    return super.finishComposingText()
                }

                override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                    stateMachine?.setComposing(false)
                    // Mark latest intent as typing so downstream policies can bias correctly
                    this@MaskedEditText.lastEvent = InputEvent.CHARACTER_TYPED
                    val ok = super.commitText(text, newCursorPosition)
                    // Adjust once immediately; prefer focus target after commit (handles multi-char paste)
                    val fmt = maskFormatter
                    if (fmt != null && selectionStart == selectionEnd) {
                        val target = cursorController.focusTarget(fmt, this@MaskedEditText.getUnmaskedText(), this@MaskedEditText.text?.length ?: 0)
                        setSelectionSafely(target)
                    }
                    return ok
                }

                // Note: We intentionally rely on key events for deletion to keep behavior consistent
                // across different IMEs and retain deterministic test behavior. If a specific IME
                // uses deleteSurroundingText without key events and needs support, we can add a
                // policy-driven handler here keyed off that IME.

                // Rely on key events by default; some IMEs use deleteSurroundingText only,
                // but we prioritize correctness in tests and common keyboards. We'll revisit if needed.

                override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                    val formatter = maskFormatter
                    if (formatter != null && selectionStart == selectionEnd) {
                        val textLen = this@MaskedEditText.text?.length ?: 0
                        val caret = selectionStart
                        // Backspace behavior: delete the previous slot value, not the literal
                        if (beforeLength > 0 && afterLength == 0) {
                            val newCaret = cursorController.beforeBackspaceReposition(caret, textLen, formatter)
                            if (newCaret != caret) setSelectionSafely(newCaret)
                        }
                        // Forward delete: if caret is on a literal, advance to the next slot
                        else if (afterLength > 0 && beforeLength == 0) {
                            val nextSlot = formatter.validPositions.firstOrNull { it >= caret }
                            if (nextSlot != null && nextSlot != caret) setSelectionSafely(nextSlot)
                        }
                    }
                    this@MaskedEditText.lastEvent = InputEvent.CHARACTER_DELETED
                    return super.deleteSurroundingText(beforeLength, afterLength)
                }

                override fun sendKeyEvent(event: KeyEvent): Boolean {
                    // Some IMEs send explicit DEL key events; mirror backspace behavior
                    if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                        val formatter = maskFormatter
                        if (formatter != null && selectionStart == selectionEnd) {
                            val textLen = this@MaskedEditText.text?.length ?: 0
                            val caret = selectionStart
                            val newCaret = cursorController.beforeBackspaceReposition(caret, textLen, formatter)
                            if (newCaret != caret) setSelectionSafely(newCaret)
                            return super.sendKeyEvent(event)
                        }
                    }
                    return super.sendKeyEvent(event)
                }
            }
        }

    class Builder(private val context: Context) {
        private var mask: String = ""
        private var notMaskedSymbol: String = "*"
        private var format: String? = null
        private var required: Boolean = false
        private var icon: Drawable? = null
        private var iconCallback: IconCallback? = null
        private var maskIconCallback: MaskIconCallback? = null
        private var stateChangeListener: InputStateMachine.InputStateListener? = null
        private var deferFocusSelection: Boolean = false

        fun mask(mask: String) = apply { this.mask = mask }

        fun notMaskedSymbol(symbol: String) = apply { this.notMaskedSymbol = symbol }

        @Deprecated("deleteChar is no longer supported", level = DeprecationLevel.WARNING)
        fun deleteChar(@Suppress("UNUSED_PARAMETER") char: String) = this

        @Deprecated("replacementChar is no longer supported", level = DeprecationLevel.WARNING)
        fun replacementChar(@Suppress("UNUSED_PARAMETER") char: String) = this

        fun format(format: String?) = apply { this.format = format }

        fun required(required: Boolean) = apply { this.required = required }

        fun icon(@DrawableRes iconRes: Int) = apply {
            context.getDrawable(iconRes)?.let {
                this.icon = DrawableCompat.wrap(it)
            }
        }

        fun icon(icon: Drawable?) = apply { this.icon = icon }

        fun iconCallback(callback: IconCallback?) = apply { this.iconCallback = callback }

        fun maskIconCallback(callback: MaskIconCallback?) =
            apply { this.maskIconCallback = callback }

        fun stateChangeListener(listener: InputStateMachine.InputStateListener?) = apply {
            this.stateChangeListener = listener
        }

        fun deferFocusSelection(enabled: Boolean) = apply { this.deferFocusSelection = enabled }

        fun build(): MaskedEditText {
            val maskPattern = this.mask
            val placeholderSymbol = this.notMaskedSymbol
            val formatPattern = this.format
            val requiredFlag = this.required
            val iconDrawable = this.icon
            val iconClick = this.iconCallback
            val maskIconClick = this.maskIconCallback
            val stateListener = this.stateChangeListener

            return MaskedEditText(context).apply {
                setMask(maskPattern)
                setNotMaskedSymbol(placeholderSymbol)
                setFormat(formatPattern)
                setRequired(requiredFlag)
                setMaskIcon(iconDrawable)
                setIconCallback(iconClick)
                setMaskIconCallback(maskIconClick)
                setStateChangeListener(stateListener)
                // Override auto-mode if explicitly requested by builder
                setDeferFocusSelection(this@Builder.deferFocusSelection)
            }
        }
    }

    interface IconCallback {
        fun onIconClick(unmaskedText: String)
    }

    interface MaskIconCallback {
        fun onMaskIconClick(maskedEditText: MaskedEditText, unmaskedText: String)
    }
}
