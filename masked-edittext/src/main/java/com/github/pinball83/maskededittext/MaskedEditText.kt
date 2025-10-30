package com.github.pinball83.maskededittext

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputFilter
import android.text.Selection
import android.text.Spanned
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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
) : AppCompatEditText(context, attrs, defStyleAttr), View.OnTouchListener, View.OnFocusChangeListener {

    private var mask: String = ""
    private var notMaskedSymbol: String = "*"
    private var format: String? = null
    private var required: Boolean = false
    private var maskIconCallback: MaskIconCallback? = null
    private var iconCallback: IconCallback? = null
    private var maskIcon: Drawable? = null

    private var maskFormatter: MaskFormatter? = null
    private var firstAllowedPosition: Int = 0
    private var lastAllowedPosition: Int = 0
    private var userFocusChangeListener: OnFocusChangeListener? = null

    private var stateChangeListener: InputStateMachine.InputStateListener? = null
    private var stateMachine: InputStateMachine? = null
    private var pendingInputEvent: InputEvent? = null
    private var adjustingSelection: Boolean = false
    private var suppressFilter: Boolean = false

    private fun MaskFormatter.cursorForNormalizedLength(normalizedLength: Int, textLength: Int): Int {
        return when {
            normalizedLength <= 0 -> firstValidPosition() ?: 0
            normalizedLength >= validPositions.size -> {
                (lastValidPosition()?.plus(1))?.coerceAtMost(textLength) ?: textLength
            }

            else -> cursorPositionFor(normalizedLength)
        }.coerceIn(0, textLength)
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
                override fun onStateChanged(oldState: InputState, newState: InputState, event: InputEvent) {
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
            firstAllowedPosition = formatter.firstValidPosition() ?: 0
            lastAllowedPosition = formatter.lastValidPosition() ?: 0
            filters = arrayOf(MaskedInputFilter())
            setOnTouchListener(this)
            super.setOnFocusChangeListener(this)

            val currentMasked = text?.toString().orEmpty()
            val reApplied = formatter.mask(getUnmaskedText(currentMasked))
            if (currentMasked != reApplied) {
                setText(reApplied)
            }
            val normalizedLength = formatter.normalize(getUnmaskedText()).length
            val cursorPosition = formatter.cursorForNormalizedLength(normalizedLength, reApplied.length)
            setSelection(cursorPosition)
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

    private fun applyMask() {
        val formatter = maskFormatter
        if (formatter == null) {
            return
        }

        val currentText = text?.toString().orEmpty()
        val unmaskedText = formatter.unmask(currentText)
        val maskedText = formatter.mask(unmaskedText)

        if (currentText != maskedText) {
            setText(maskedText)
            setSelection(getValidCursorPosition(maskedText.length))
        }
    }

    private fun applyMaskToText(input: String): String {
        val formatter = maskFormatter ?: return input
        return formatter.mask(input)
    }

    fun getUnmaskedText(maskedText: String = text?.toString().orEmpty()): String {
        val formatter = maskFormatter ?: return maskedText
        return formatter.unmask(maskedText)
    }

    fun getFormattedText(): String {
        val formatter = maskFormatter ?: return getUnmaskedText().trim()
        return formatter.formatOutput(getUnmaskedText())
    }

    private fun getValidCursorPosition(position: Int): Int {
        val formatter = maskFormatter ?: return position
        return formatter.nearestValidPosition(position)
    }

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
            if (TextUtils.isEmpty(text)) {
                val formatter = maskFormatter
                if (formatter != null) {
                    setSelection(formatter.firstValidPosition() ?: 0)
                }
            }
        } else {
            stateMachine?.processEvent(InputEvent.FOCUS_LOST)
        }

        userFocusChangeListener?.onFocusChange(v, hasFocus)
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
            val cursorPosition = formatter.cursorForNormalizedLength(normalizedLength, maskedText.length)
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

            val slots = formatter.validPositions

            fun slotIndexForPosition(pos: Int): Int {
                if (slots.isEmpty()) return pos.coerceAtLeast(0)
                val idx = slots.indexOfFirst { it >= pos }
                return if (idx == -1) slots.size else idx
            }

            val startSlot = slotIndexForPosition(dstart)
            val endSlot = slotIndexForPosition(dend)

            val newUnmasked = if (src.isEmpty()) {
                var from = startSlot.coerceAtMost(destUnmasked.length)
                var to = endSlot.coerceIn(from, destUnmasked.length)

                if (from == to) {
                    if (dstart > 0) {
                        val targetSlot = (startSlot - 1).coerceAtLeast(0)
                        from = targetSlot.coerceAtMost(destUnmasked.length)
                        to = (targetSlot + 1).coerceIn(from, destUnmasked.length)
                    } else if (endSlot < destUnmasked.length) {
                        from = endSlot.coerceAtMost(destUnmasked.length)
                        to = (endSlot + 1).coerceIn(from, destUnmasked.length)
                    }
                }

                val trimmed = destUnmasked.removeRange(from, to)
                formatter.normalize(trimmed)
            } else {
                val inserted = destUnmasked.substring(0, startSlot) + cleanedSrc + destUnmasked.substring(endSlot)
                formatter.normalize(inserted)
            }

            val maskedNew = formatter.mask(newUnmasked)

            // If nothing effectively changes, let the system handle it
            val naiveProposed = destMasked.substring(0, dstart) + src + destMasked.substring(dend)
            if (maskedNew == naiveProposed) {
                return null
            }

            val normalizedLength = formatter.normalize(newUnmasked).length
            val cursorPosition = formatter.cursorForNormalizedLength(normalizedLength, maskedNew.length)

            suppressFilter = true
            try {
                val editableDest = dest as? Editable
                if (editableDest != null) {
                    editableDest.replace(0, editableDest.length, maskedNew)
                    Selection.setSelection(editableDest, cursorPosition)
                } else {
                    setText(maskedNew)
                    adjustingSelection = true
                    setSelection(cursorPosition)
                    adjustingSelection = false
                }
            } finally {
                suppressFilter = false
            }

            val replacementLength = (dend - dstart).coerceAtLeast(0)
            val replacementStart = dstart.coerceIn(0, maskedNew.length)
            val replacementEnd = (replacementStart + replacementLength).coerceAtMost(maskedNew.length)
            return maskedNew.substring(replacementStart, replacementEnd)
        }
    }


    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        super.onTextChanged(text, start, before, count)

        val formatter = maskFormatter
        if (formatter != null && text != null) {
            val current = text.toString()
            val unmasked = getUnmaskedText(current)
            val remasked = formatter.mask(unmasked)

            // Re-apply mask if needed to keep template stable
            if (current != remasked) {
                setText(remasked)
                val normalizedLength = formatter.normalize(unmasked).length
                val cursorPosition = formatter.cursorForNormalizedLength(normalizedLength, remasked.length)
                if (!adjustingSelection) {
                    adjustingSelection = true
                    setSelection(cursorPosition)
                    adjustingSelection = false
                }
                return
            }


        }

        val event = pendingInputEvent
        if (event != null) {
            stateMachine?.processEvent(event)
            pendingInputEvent = null
        } else if (before != 0 || count != 0) {
            stateMachine?.processEvent(InputEvent.VALIDATE)
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (adjustingSelection) {
            super.onSelectionChanged(selStart, selEnd)
            return
        }

        val formatter = maskFormatter
        if (formatter != null && selStart == selEnd) {
            val textLength = text?.length ?: 0
            val lastSlot = formatter.lastValidPosition() ?: -1
            val allowedTrailing = (lastSlot + 1).coerceAtMost(textLength)

            if (selStart <= lastSlot) {
                val validPosition = formatter.nearestValidPosition(selStart.coerceAtLeast(0))
                val cappedPosition = validPosition.coerceIn(0, textLength)
                if (cappedPosition != selStart) {
                    adjustingSelection = true
                    setSelection(cappedPosition)
                    adjustingSelection = false
                    return
                }
            } else if (selStart > allowedTrailing) {
                val capped = allowedTrailing.coerceIn(0, textLength)
                if (capped != selStart) {
                    adjustingSelection = true
                    setSelection(capped)
                    adjustingSelection = false
                    return
                }
            }
        }

        super.onSelectionChanged(selStart, selEnd)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? =
        super.onCreateInputConnection(outAttrs)

    class Builder(private val context: Context) {
        private var mask: String = ""
        private var notMaskedSymbol: String = "*"
        private var format: String? = null
        private var required: Boolean = false
        private var icon: Drawable? = null
        private var iconCallback: IconCallback? = null
        private var maskIconCallback: MaskIconCallback? = null
        private var stateChangeListener: InputStateMachine.InputStateListener? = null

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

        fun maskIconCallback(callback: MaskIconCallback?) = apply { this.maskIconCallback = callback }

        fun stateChangeListener(listener: InputStateMachine.InputStateListener?) = apply {
            this.stateChangeListener = listener
        }

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
