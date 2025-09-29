package com.github.pinball83.maskededittext.compose

import androidx.compose.runtime.Immutable
import com.github.pinball83.maskededittext.InputEvent
import com.github.pinball83.maskededittext.InputState

/**
 * Configuration options for MaskedTextField
 */
@Immutable
data class MaskedOptions(
    val mask: String,
    val notMaskedSymbol: Char = '*',
    val format: String? = null,
    val onIconClick: ((String) -> Unit)? = null,
    val onStateChanged: ((InputState, InputState, InputEvent) -> Unit)? = null
) {
    companion object {
        /**
         * Creates MaskedOptions for phone number input
         */
        fun phone(
            onStateChanged: ((InputState, InputState, InputEvent) -> Unit)? = null
        ) = MaskedOptions(
            mask = "8 (***) *** **-**",
            notMaskedSymbol = '*',
            onStateChanged = onStateChanged
        )
        
        /**
         * Creates MaskedOptions for credit card input
         */
        fun creditCard(
            onStateChanged: ((InputState, InputState, InputEvent) -> Unit)? = null
        ) = MaskedOptions(
            mask = "**** **** **** ****",
            notMaskedSymbol = '*',
            onStateChanged = onStateChanged
        )
        
        /**
         * Creates MaskedOptions for social security number input
         */
        fun socialSecurity(
            onStateChanged: ((InputState, InputState, InputEvent) -> Unit)? = null
        ) = MaskedOptions(
            mask = "***-**-****",
            notMaskedSymbol = '*',
            onStateChanged = onStateChanged
        )
        
        /**
         * Creates MaskedOptions for date input (MM/DD/YY)
         */
        fun date(
            onStateChanged: ((InputState, InputState, InputEvent) -> Unit)? = null
        ) = MaskedOptions(
            mask = "**/**/**",
            notMaskedSymbol = '*',
            onStateChanged = onStateChanged
        )
        
        /**
         * Creates MaskedOptions for time input (HH:MM)
         */
        fun time(
            onStateChanged: ((InputState, InputState, InputEvent) -> Unit)? = null
        ) = MaskedOptions(
            mask = "**:**",
            notMaskedSymbol = '*',
            onStateChanged = onStateChanged
        )
        
        /**
         * Creates MaskedOptions for custom mask
         */
        fun custom(
            mask: String,
            notMaskedSymbol: Char = '*',
            format: String? = null,
            onIconClick: ((String) -> Unit)? = null,
            onStateChanged: ((InputState, InputState, InputEvent) -> Unit)? = null
        ) = MaskedOptions(
            mask = mask,
            notMaskedSymbol = notMaskedSymbol,
            format = format,
            onIconClick = onIconClick,
            onStateChanged = onStateChanged
        )
    }
}

/**
 * Visual configuration options for MaskedTextField
 */
@Immutable
data class MaskedVisualOptions(
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val singleLine: Boolean = true,
    val maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    val minLines: Int = 1,
    val isError: Boolean = false
)

/**
 * Input configuration options for MaskedTextField
 */
@Immutable
data class MaskedInputOptions(
    val keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    val keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default
)