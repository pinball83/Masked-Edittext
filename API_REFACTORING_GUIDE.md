# 🔄 MaskedTextField API Refactoring Guide

## Overview

The MaskedTextField API has been refactored to reduce parameter count and improve maintainability by grouping related options into data classes. This makes the API cleaner, more discoverable, and easier to use.

## Key Changes

### Before (Old API - 20+ parameters)
```kotlin
MaskedTextField(
    value = phoneNumber,
    onValueChange = { phoneNumber = it },
    mask = "8 (***) *** **-**",
    notMaskedSymbol = '*',
    modifier = Modifier.fillMaxWidth(),
    enabled = true,
    readOnly = false,
    textStyle = LocalTextStyle.current,
    label = { Text("Phone Number") },
    placeholder = null,
    leadingIcon = null,
    trailingIcon = null,
    supportingText = null,
    isError = false,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    keyboardActions = KeyboardActions.Default,
    singleLine = true,
    maxLines = 1,
    minLines = 1,
    interactionSource = remember { MutableInteractionSource() },
    colors = TextFieldDefaults.colors(),
    onIconClick = null,
    format = null,
    onStateChanged = null
)
```

### After (New API - 10 parameters)
```kotlin
MaskedTextField(
    value = phoneNumber,
    onValueChange = { phoneNumber = it },
    maskedOptions = MaskedOptions.phone(),
    modifier = Modifier.fillMaxWidth(),
    textStyle = LocalTextStyle.current,
    label = { Text("Phone Number") },
    placeholder = null,
    leadingIcon = null,
    trailingIcon = null,
    supportingText = null,
    visualOptions = MaskedVisualOptions(),
    inputOptions = MaskedInputOptions(
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
    ),
    interactionSource = remember { MutableInteractionSource() },
    colors = TextFieldDefaults.colors()
)
```

## New Data Classes

### 1. MaskedOptions
Groups mask-related configuration:
```kotlin
data class MaskedOptions(
    val mask: String,
    val notMaskedSymbol: Char = '*',
    val format: String? = null,
    val onIconClick: ((String) -> Unit)? = null,
    val onStateChanged: ((InputState, InputState, InputEvent) -> Unit)? = null
)
```

### 2. MaskedVisualOptions
Groups visual/display configuration:
```kotlin
data class MaskedVisualOptions(
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val singleLine: Boolean = true,
    val maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    val minLines: Int = 1,
    val isError: Boolean = false
)
```

### 3. MaskedInputOptions
Groups input-related configuration:
```kotlin
data class MaskedInputOptions(
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    val keyboardActions: KeyboardActions = KeyboardActions.Default
)
```

## Predefined Options

### Common Use Cases Made Simple

#### Phone Numbers
```kotlin
// Before
MaskedTextField(
    value = phone,
    onValueChange = { phone = it },
    mask = "8 (***) *** **-**",
    notMaskedSymbol = '*',
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
)

// After
MaskedTextField(
    value = phone,
    onValueChange = { phone = it },
    maskedOptions = MaskedOptions.phone(),
    inputOptions = MaskedInputOptions(
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
    )
)
```

#### Credit Cards
```kotlin
// Before
MaskedTextField(
    value = card,
    onValueChange = { card = it },
    mask = "**** **** **** ****",
    notMaskedSymbol = '*',
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
)

// After
MaskedTextField(
    value = card,
    onValueChange = { card = it },
    maskedOptions = MaskedOptions.creditCard(),
    inputOptions = MaskedInputOptions(
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
)
```

#### Social Security Numbers
```kotlin
// Before
MaskedTextField(
    value = ssn,
    onValueChange = { ssn = it },
    mask = "***-**-****",
    notMaskedSymbol = '*',
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
)

// After
MaskedTextField(
    value = ssn,
    onValueChange = { ssn = it },
    maskedOptions = MaskedOptions.socialSecurity(),
    inputOptions = MaskedInputOptions(
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
)
```

#### Date Input
```kotlin
// Before
MaskedTextField(
    value = date,
    onValueChange = { date = it },
    mask = "**/**/**",
    notMaskedSymbol = '*',
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
)

// After
MaskedTextField(
    value = date,
    onValueChange = { date = it },
    maskedOptions = MaskedOptions.date(),
    inputOptions = MaskedInputOptions(
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
)
```

#### Time Input
```kotlin
// Before
MaskedTextField(
    value = time,
    onValueChange = { time = it },
    mask = "**:**",
    notMaskedSymbol = '*',
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
)

// After
MaskedTextField(
    value = time,
    onValueChange = { time = it },
    maskedOptions = MaskedOptions.time(),
    inputOptions = MaskedInputOptions(
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
)
```

## Custom Configurations

### Custom Mask with State Listener
```kotlin
// Before
MaskedTextField(
    value = custom,
    onValueChange = { custom = it },
    mask = "Q***************",
    notMaskedSymbol = '*',
    format = "[1][2][3] [4][5][6]-[7][8]-[10][9]",
    onStateChanged = { oldState, newState, event ->
        println("State: $oldState -> $newState")
    }
)

// After
MaskedTextField(
    value = custom,
    onValueChange = { custom = it },
    maskedOptions = MaskedOptions.custom(
        mask = "Q***************",
        format = "[1][2][3] [4][5][6]-[7][8]-[10][9]",
        onStateChanged = { oldState, newState, event ->
            println("State: $oldState -> $newState")
        }
    )
)
```

### Visual Customization
```kotlin
// Before
MaskedTextField(
    value = input,
    onValueChange = { input = it },
    mask = "***-***",
    enabled = false,
    readOnly = true,
    isError = true,
    singleLine = false,
    maxLines = 3
)

// After
MaskedTextField(
    value = input,
    onValueChange = { input = it },
    maskedOptions = MaskedOptions.custom("***-***"),
    visualOptions = MaskedVisualOptions(
        enabled = false,
        readOnly = true,
        isError = true,
        singleLine = false,
        maxLines = 3
    )
)
```

## State Management Updates

### MaskedTextFieldState
```kotlin
// Before
val state = rememberMaskedTextFieldState(
    mask = "8 (***) *** **-**",
    notMaskedSymbol = '*',
    onStateChanged = { oldState, newState, event -> }
)

// After
val state = rememberMaskedTextFieldState(
    maskedOptions = MaskedOptions.phone { oldState, newState, event -> }
)
```

## Migration Benefits

### 1. **Reduced Parameter Count**
- From 20+ parameters to 10 core parameters
- Grouped related options logically
- Easier to understand and maintain

### 2. **Better Discoverability**
- Predefined options for common use cases
- Clear separation of concerns
- IDE autocomplete shows relevant options

### 3. **Type Safety**
- Immutable data classes prevent accidental modifications
- Compile-time validation of option combinations
- Better error messages

### 4. **Extensibility**
- Easy to add new options without breaking existing API
- Companion object functions for common patterns
- Custom configurations remain flexible

### 5. **Consistency**
- Uniform API across all mask types
- Consistent naming conventions
- Predictable behavior

## Backward Compatibility

The old API is **deprecated but still functional**. You can migrate gradually:

1. **Phase 1**: Update to use `MaskedOptions.custom()` for existing masks
2. **Phase 2**: Replace with predefined options where applicable
3. **Phase 3**: Group visual and input options as needed

## Best Practices

### 1. Use Predefined Options When Possible
```kotlin
// Good
MaskedOptions.phone()
MaskedOptions.creditCard()
MaskedOptions.socialSecurity()

// Avoid when predefined option exists
MaskedOptions.custom("8 (***) *** **-**")
```

### 2. Group Related Configurations
```kotlin
// Good - grouped visual options
val visualOptions = MaskedVisualOptions(
    enabled = isFormEnabled,
    readOnly = isReadOnlyMode,
    isError = hasValidationError
)

// Avoid - scattered individual parameters
```

### 3. Reuse Option Instances
```kotlin
// Good - reuse across multiple fields
val phoneOptions = MaskedOptions.phone()
val inputOptions = MaskedInputOptions(
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
)

// Use in multiple places
MaskedTextField(value = phone1, onValueChange = {}, maskedOptions = phoneOptions, inputOptions = inputOptions)
MaskedTextField(value = phone2, onValueChange = {}, maskedOptions = phoneOptions, inputOptions = inputOptions)
```

## Summary

The refactored API provides:
- ✅ **Cleaner interface** with fewer parameters
- ✅ **Better organization** with logical grouping
- ✅ **Predefined options** for common use cases
- ✅ **Type safety** with immutable data classes
- ✅ **Extensibility** for future enhancements
- ✅ **Backward compatibility** during migration

This refactoring makes the MaskedTextField more maintainable, discoverable, and easier to use while preserving all existing functionality.