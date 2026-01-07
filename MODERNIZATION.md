# Masked EditText Modernization

This document outlines the modernization of the Masked EditText library, including the conversion to Kotlin, addition of state machine logic, and Jetpack Compose support.

## 🚀 New Features

### 1. State Machine Architecture
- **InputState**: Enum defining different input states (EMPTY, PARTIAL, COMPLETE, INVALID, FOCUSED, UNFOCUSED)
- **InputEvent**: Enum defining events that trigger state transitions
- **InputStateMachine**: Core state machine managing input validation and state transitions

### 2. Jetpack Compose Support
- **MaskedTextField**: Compose implementation of the masked input field
- **MaskedTextFieldState**: State holder with integrated state machine
- **MaskedTextFieldDemo**: Comprehensive demo showcasing all features

### 3. Kotlin Conversion
- All Java code converted to modern Kotlin
- Leverages Kotlin features like data classes, null safety, and extension functions
- Improved type safety and reduced boilerplate

## 📋 API Overview

### Traditional View System (Backward Compatible)

```kotlin
val maskedEditText = MaskedEditText.Builder(context)
    .mask("8 (***) *** **-**")
    .notMaskedSymbol("*")
    .icon(R.drawable.ic_clear)
    .iconCallback { unmaskedText -> 
        // Handle icon click
    }
    .build()
```

### Jetpack Compose

```kotlin
@Composable
fun MyScreen() {
    var phoneNumber by remember { mutableStateOf("") }
    
    MaskedTextField(
        value = phoneNumber,
        onValueChange = { phoneNumber = it },
        mask = "8 (***) *** **-**",
        notMaskedSymbol = '*',
        label = { Text("Phone Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
    )
}
```

### Advanced State Management

```kotlin
@Composable
fun AdvancedExample() {
    val phoneState = rememberMaskedTextFieldState(
        mask = "8 (***) *** **-**",
        notMaskedSymbol = '*',
        onStateChanged = { oldState, newState, event ->
            // Handle state changes
        }
    )
    
    OutlinedTextField(
        value = phoneState.textFieldValue,
        onValueChange = { phoneState.updateValue(it) },
        // ... other parameters
    )
    
    // Access state information
    Text("Is Complete: ${phoneState.isComplete}")
    Text("Current State: ${phoneState.currentState}")
    Text("Unmasked Value: ${phoneState.unmaskedValue}")
}
```

## 🎯 State Machine States

| State | Description |
|-------|-------------|
| `EMPTY` | Input field is empty or contains only mask characters |
| `PARTIAL` | Input field contains partial valid input |
| `COMPLETE` | Input field is completely filled with valid input |
| `INVALID` | Input field contains invalid characters or format |
| `FOCUSED` | Input field is in focus and ready for input |
| `UNFOCUSED` | Input field has lost focus |

## 🔄 State Transitions

The state machine handles the following events:
- `CHARACTER_TYPED`: User types a character
- `CHARACTER_DELETED`: User deletes a character
- `TEXT_PASTED`: User pastes text
- `FOCUS_GAINED`: Input field gains focus
- `FOCUS_LOST`: Input field loses focus
- `TEXT_SET`: Text is set programmatically
- `INPUT_CLEARED`: Input is cleared
- `VALIDATE`: Validation is triggered

## 📱 Supported Mask Patterns

### Phone Numbers
```kotlin
mask = "8 (***) *** **-**"
// Result: 8 (123) 456 78-90
```

### Credit Cards
```kotlin
mask = "**** **** **** ****"
// Result: 1234 5678 9012 3456
```

### Social Security Numbers
```kotlin
mask = "***-**-****"
// Result: 123-45-6789
```

### Custom Patterns
```kotlin
mask = "Q***************"
// Result: QABCDEFGHIJKLMNO
```

### Date/Time
```kotlin
mask = "**/**/**"  // Date
mask = "**:**"     // Time
```

## 🎨 Custom Formatting

You can reorder output using format strings:

```kotlin
MaskedTextField(
    mask = "8 (***) *** **-**",
    format = "[1][2][3] [4][5][6]-[7][8]-[10][9]", // Swap last two digits
    // Input: 1234567890
    // Masked: 8 (123) 456 78-90
    // Formatted: 123 456-78-09
)
```

## 🧪 Testing

The library includes comprehensive tests:

### Unit Tests
- `MaskedEditTextTest`: Tests for the original functionality
- `InputStateMachineTest`: Tests for state machine logic
- `MaskedInputFilterTest`: Tests for input filtering
- `MaskedTextFieldStateTest`: Tests for Compose state management

### Running Tests
```bash
./gradlew test
```

## 🔧 Migration Guide

### From Version 1.x to 2.x

1. **Update Dependencies**:
```gradle
implementation 'com.github.pinball83:masked-edittext:2.0.0'
```

2. **Kotlin Migration**: The library is now written in Kotlin, but Java interop is maintained.

3. **New Features**: Take advantage of the state machine and Compose support:
```kotlin
// Old way (still works)
val maskedEditText = MaskedEditText.Builder(context)
    .mask("***-***-****")
    .build()

// New way with state machine
val stateMachine = InputStateMachine(maskedEditText) { oldState, newState, event ->
    // Handle state changes
}

// Compose way
MaskedTextField(
    value = value,
    onValueChange = { value = it },
    mask = "***-***-****"
)
```

## 🏗️ Architecture

```
┌─────────────────────────────────────┐
│           Compose Layer             │
│  ┌─────────────────────────────────┐│
│  │      MaskedTextField            ││
│  │   MaskedTextFieldState          ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│         State Machine Layer         │
│  ┌─────────────────────────────────┐│
│  │    InputStateMachine            ││
│  │    InputState / InputEvent      ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│          View Layer (Legacy)        │
│  ┌─────────────────────────────────┐│
│  │      MaskedEditText             ││
│  │    MaskedInputFilter            ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

## 🎯 Benefits

1. **Type Safety**: Kotlin's null safety and type system prevent common errors
2. **State Management**: Clear state machine makes input validation predictable
3. **Modern UI**: Jetpack Compose support for modern Android development
4. **Backward Compatibility**: Existing code continues to work
5. **Better Testing**: Comprehensive test coverage with modern testing frameworks
6. **Performance**: Optimized for modern Android runtime

## 📚 Examples

See `MaskedTextFieldDemo.kt` for comprehensive examples of all features.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## 📄 License

This project maintains the same license as the original Masked EditText library.