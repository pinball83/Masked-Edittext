package com.github.pinball83.maskededittext.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import com.github.pinball83.maskededittext.InputEvent

/**
 * Jetpack Compose implementation of MaskedEditText backed by the shared formatter and state holder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    maskedOptions: MaskedOptions,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    visualOptions: MaskedVisualOptions = MaskedVisualOptions(),
    inputOptions: MaskedInputOptions = MaskedInputOptions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    val state = rememberMaskedTextFieldState(initialValue = value, maskedOptions = maskedOptions)

    LaunchedEffect(value) {
        if (value != state.unmaskedValue) {
            state.updateValue(value)
        }
    }

    MaskedTextField(
        state = state,
        onValueChange = onValueChange,
        maskedOptions = maskedOptions,
        modifier = modifier,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        visualOptions = visualOptions,
        inputOptions = inputOptions,
        interactionSource = interactionSource,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskedTextField(
    state: MaskedTextFieldState,
    onValueChange: (String) -> Unit,
    maskedOptions: MaskedOptions,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    visualOptions: MaskedVisualOptions = MaskedVisualOptions(),
    inputOptions: MaskedInputOptions = MaskedInputOptions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    val resolvedTrailingIcon = trailingIcon ?: maskedOptions.onIconClick?.let { callback ->
        {
            IconButton(onClick = { callback(state.unmaskedValue) }) {
                Text("Go")
            }
        }
    }

    OutlinedTextField(
        value = state.textFieldValue,
        onValueChange = { newValue ->
            // Ignore extraneous backspace when already empty to prevent mask artifact
            if (state.unmaskedValue.isEmpty() && newValue.text.length < state.textFieldValue.text.length) {
                onValueChange(state.unmaskedValue)
                return@OutlinedTextField
            }
            val previousValue = state.unmaskedValue
            state.updateValue(newValue)
            val event = when {
                state.unmaskedValue.length < previousValue.length -> InputEvent.CHARACTER_DELETED
                state.unmaskedValue.length - previousValue.length > 1 -> InputEvent.TEXT_PASTED
                else -> InputEvent.CHARACTER_TYPED
            }
            if (event != InputEvent.CHARACTER_DELETED) {
                // no-op: events already handled inside state, hook kept for parity
            }
            onValueChange(state.unmaskedValue)
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState -> state.onFocusChanged(focusState.isFocused) },
        enabled = visualOptions.enabled,
        readOnly = visualOptions.readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = resolvedTrailingIcon,
        supportingText = supportingText,
        isError = visualOptions.isError,
        keyboardOptions = inputOptions.keyboardOptions,
        keyboardActions = inputOptions.keyboardActions,
        singleLine = visualOptions.singleLine,
        maxLines = visualOptions.maxLines,
        minLines = visualOptions.minLines,
        interactionSource = interactionSource,
        colors = colors
    )
}
