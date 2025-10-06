package com.github.pinball83.maskededittext

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
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
            val cursorPosition = if (normalizedLength == 0) {
                formatter.firstValidPosition() ?: 0
            } else {
                formatter.cursorPositionFor(normalizedLength)
            }
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
            val cursorPosition = if (normalizedLength == 0) {
                formatter.firstValidPosition() ?: 0
            } else {
                formatter.cursorPositionFor(normalizedLength)
            }
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
            val formatter = maskFormatter ?: return null
            if (source == null) return null

            val currentText = dest?.toString() ?: ""
            val proposed = currentText.substring(0, dstart) +
                source.subSequence(start, end) +
                currentText.substring(dend)

            val unmaskedNew = formatter.unmask(proposed)
            val maskedNew = formatter.mask(unmaskedNew)

            when {
                source.isEmpty() -> stateMachine?.processEvent(InputEvent.CHARACTER_DELETED)
                source.length > 1 -> stateMachine?.processEvent(InputEvent.TEXT_PASTED)
                else -> stateMachine?.processEvent(InputEvent.CHARACTER_TYPED)
            }

            return if (maskedNew == proposed) {
                null
            } else {
                maskedNew.substring(dstart, dstart + maskedNew.length - currentText.length + (dend - dstart))
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
            return MaskedEditText(context).apply {
                setMask(mask)
                setNotMaskedSymbol(notMaskedSymbol)
                setFormat(format)
                setRequired(required)
                setMaskIcon(icon)
                setIconCallback(iconCallback)
                setMaskIconCallback(maskIconCallback)
                setStateChangeListener(stateChangeListener)
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
