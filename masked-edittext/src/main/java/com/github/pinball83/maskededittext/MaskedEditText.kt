package com.github.pinball83.maskededittext

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.drawable.DrawableCompat
import com.thrd.maskededittext.R
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

/**
 * Modernized Kotlin version of MaskedEditText with state machine support
 */
class MaskedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr), View.OnTouchListener, View.OnFocusChangeListener {

    private var mask: String = ""
    private var notMaskedSymbol: String = "*"
    private var deleteChar: String = ""
    private var replacementChar: String = ""
    private var format: String? = null
    private var required: Boolean = false
    private var maskIconCallback: MaskIconCallback? = null
    private var iconCallback: IconCallback? = null
    private var maskIcon: Drawable? = null
    
    private val listValidCursorPositions = ArrayList<Int>()
    private var firstAllowedPosition: Int = 0
    private var lastAllowedPosition: Int = 0
    private var onFocusChangeListener: OnFocusChangeListener? = null
    
    // State machine integration
    private var stateMachine: InputStateMachine? = null
    private var stateChangeListener: InputStateMachine.InputStateListener? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.MaskedEditText, defStyleAttr, 0).apply {
            try {
                mask = getString(R.styleable.MaskedEditText_mask) ?: ""
                notMaskedSymbol = getString(R.styleable.MaskedEditText_notMaskedSymbol) ?: "*"
                deleteChar = getString(R.styleable.MaskedEditText_deleteChar) ?: ""
                replacementChar = getString(R.styleable.MaskedEditText_replacementChar) ?: ""
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
        
        initializeMask()
        setupStateMachine()
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
    }

    fun setStateChangeListener(listener: InputStateMachine.InputStateListener?) {
        stateChangeListener = listener
    }

    fun getCurrentInputState(): InputState? = stateMachine?.getCurrentState()

    fun isInputComplete(): Boolean = stateMachine?.isComplete() ?: false

    fun isInputValid(): Boolean = stateMachine?.isValid() ?: true

    fun isInputEmpty(): Boolean = stateMachine?.isEmpty() ?: true

    private fun initializeMask() {
        if (mask.isEmpty()) return
        
        listValidCursorPositions.clear()
        
        // Find valid cursor positions
        mask.forEachIndexed { index, char ->
            if (char.toString() == notMaskedSymbol) {
                listValidCursorPositions.add(index)
            }
        }
        
        if (listValidCursorPositions.isNotEmpty()) {
            firstAllowedPosition = listValidCursorPositions.first()
            lastAllowedPosition = listValidCursorPositions.last()
        }
        
        // Set up input filter
        filters = arrayOf(MaskedInputFilter())
        
        // Set up touch and focus listeners
        setOnTouchListener(this)
        super.setOnFocusChangeListener(this)
        
        // Apply initial mask
        applyMask()
    }

    private fun applyMask() {
        if (mask.isEmpty()) return
        
        val currentText = text?.toString() ?: ""
        val unmaskedText = getUnmaskedText(currentText)
        val maskedText = applyMaskToText(unmaskedText)
        
        if (currentText != maskedText) {
            setText(maskedText)
            setSelection(getValidCursorPosition(maskedText.length))
        }
    }

    private fun applyMaskToText(input: String): String {
        if (mask.isEmpty()) return input
        
        val result = StringBuilder(mask.replace(notMaskedSymbol, " "))
        var inputIndex = 0
        
        for (position in listValidCursorPositions) {
            if (inputIndex < input.length && position < result.length) {
                result[position] = input[inputIndex]
                inputIndex++
            }
        }
        
        return result.toString()
    }

    fun getUnmaskedText(maskedText: String = text?.toString() ?: ""): String {
        if (mask.isEmpty()) return maskedText
        
        val result = StringBuilder()
        
        for (position in listValidCursorPositions) {
            if (position < maskedText.length) {
                val char = maskedText[position]
                if (char != ' ' && char.toString() != deleteChar) {
                    result.append(char)
                }
            }
        }
        
        return result.toString()
    }

    private fun getValidCursorPosition(position: Int): Int {
        if (listValidCursorPositions.isEmpty()) return position
        
        // Find the closest valid position
        var closestPosition = firstAllowedPosition
        var minDistance = Int.MAX_VALUE
        
        for (validPosition in listValidCursorPositions) {
            val distance = kotlin.math.abs(validPosition - position)
            if (distance < minDistance) {
                minDistance = distance
                closestPosition = validPosition
            }
        }
        
        return closestPosition
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP && maskIcon != null) {
            val iconStart = width - paddingRight - maskIcon!!.intrinsicWidth
            if (event.x >= iconStart) {
                val unmaskedText = getUnmaskedText()
                maskIconCallback?.onMaskIconClick(v as MaskedEditText, unmaskedText)
                iconCallback?.onIconClick(unmaskedText)
                return true
            }
        }
        return false
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            stateMachine?.processEvent(InputEvent.FOCUS_GAINED)
            // Position cursor at first valid position if text is empty
            if (TextUtils.isEmpty(text)) {
                setSelection(firstAllowedPosition)
            }
        } else {
            stateMachine?.processEvent(InputEvent.FOCUS_LOST)
        }
        
        onFocusChangeListener?.onFocusChange(v, hasFocus)
    }

    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        onFocusChangeListener = l
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

    /**
     * Input filter for masked text
     */
    private inner class MaskedInputFilter : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            if (source == null || mask.isEmpty()) return null
            
            val currentText = dest?.toString() ?: ""
            val newText = currentText.substring(0, dstart) + 
                         source.subSequence(start, end) + 
                         currentText.substring(dend)
            
            val unmaskedNew = getUnmaskedText(newText)
            val maskedNew = applyMaskToText(unmaskedNew)
            
            // Trigger state machine events
            when {
                source.isEmpty() -> stateMachine?.processEvent(InputEvent.CHARACTER_DELETED)
                source.length > 1 -> stateMachine?.processEvent(InputEvent.TEXT_PASTED)
                else -> stateMachine?.processEvent(InputEvent.CHARACTER_TYPED)
            }
            
            return if (maskedNew == newText) null else maskedNew.substring(dstart, dstart + maskedNew.length - currentText.length + (dend - dstart))
        }
    }

    /**
     * Builder pattern for creating MaskedEditText instances
     */
    class Builder(private val context: Context) {
        private var mask: String = ""
        private var notMaskedSymbol: String = "*"
        private var deleteChar: String = ""
        private var replacementChar: String = ""
        private var format: String? = null
        private var required: Boolean = false
        private var icon: Drawable? = null
        private var iconCallback: IconCallback? = null
        private var maskIconCallback: MaskIconCallback? = null
        private var stateChangeListener: InputStateMachine.InputStateListener? = null

        fun mask(mask: String) = apply { this.mask = mask }
        
        fun notMaskedSymbol(symbol: String) = apply { this.notMaskedSymbol = symbol }
        
        fun deleteChar(char: String) = apply { this.deleteChar = char }
        
        fun replacementChar(char: String) = apply { this.replacementChar = char }
        
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
                this@apply.deleteChar = this@Builder.deleteChar
                this@apply.replacementChar = this@Builder.replacementChar
                setFormat(format)
                setRequired(required)
                setMaskIcon(icon)
                setIconCallback(iconCallback)
                setMaskIconCallback(maskIconCallback)
                setStateChangeListener(stateChangeListener)
            }
        }
    }

    /**
     * Callback interface for icon clicks
     */
    interface IconCallback {
        fun onIconClick(unmaskedText: String)
    }

    /**
     * Callback interface for mask icon clicks (legacy)
     */
    interface MaskIconCallback {
        fun onMaskIconClick(maskedEditText: MaskedEditText, unmaskedText: String)
    }
}