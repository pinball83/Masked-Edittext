# Masked-Edittext

Modern, Kotlin-first input masking for Android with both classic View and Jetpack Compose APIs. This refactor keeps the original MaskedEditText API compatible while adding a shared mask core, a state machine for predictable behavior, and a composable `MaskedTextField`.

Highlights:
- Kotlin implementation with Java interop preserved
- View widget `MaskedEditText` (backward compatible)
- Jetpack Compose `MaskedTextField` with `MaskedOptions` and state holder
- Cursor policy and state machine for reliable caret movement and validation
- Extensive unit tests (Robolectric + Compose)

Demo apps live in `demo_app` (Views) and `demo_app_compose` (Compose).

## Installation

This repository ships as a Gradle module. Choose one of the following:

- Project dependency (recommended for this repo):
  - In `settings.gradle`: `include(":masked-edittext")`
  - In your app module: `implementation(project(":masked-edittext"))`

- Local Maven snapshot for integration testing:
  - Run: `./gradlew :masked-edittext:assembleRelease` (optional) and `./gradlew :masked-edittext:publishToMavenLocal`
  - Add `mavenLocal()` to repositories in your consuming project
  - Use the published coordinates printed by Gradle for the snapshot

Note: The legacy Maven Central coordinates shown in older READMEs refer to the original library and may not reflect this refactor. Prefer the module or local snapshot flow above.

## Usage (Views)

XML

```xml
<com.github.pinball83.maskededittext.MaskedEditText
    android:id="@+id/masked_edit_text"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:inputType="number"
    app:mask="8 (***) *** **-**"
    app:notMaskedSymbol="*"
    app:format="[1][2][3] [4][5][6]-[7][8]-[10][9]"
    app:maskIcon="@drawable/ic_clear"
    app:required="false"/>
```

Kotlin

```kotlin
val maskedEditText = MaskedEditText.Builder(context)
    .mask("8 (***) *** **-**")
    .notMaskedSymbol("*")
    .format("[1][2][3] [4][5][6]-[7][8]-[10][9]")
    .icon(R.drawable.ic_clear)
    .iconCallback { unmasked -> /* handle click */ }
    .stateChangeListener { old, new, event -> /* observe state */ }
    .build()

maskedEditText.setMaskedText("5551235567")
val unmasked = maskedEditText.getUnmaskedText()   // 5551235567
val formatted = maskedEditText.getFormattedText() // respects app:format when set
```

Supported attributes: `mask`, `notMaskedSymbol`, `format`, `maskIcon`, `required`.

Deprecated/no-op attributes kept for XML compatibility: `replacementChar`, `deleteChar`, `maskIconColor`.

## Usage (Jetpack Compose)

Idiomatic API with grouped options and a reusable state holder.

```kotlin
@Composable
fun PhoneField() {
    var phone by remember { mutableStateOf("") }

    MaskedTextField(
        value = phone,
        onValueChange = { phone = it },
        maskedOptions = MaskedOptions.phone(),
        inputOptions = MaskedInputOptions(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        ),
        label = { Text("Phone") }
    )
}
```

Advanced: control state and react to transitions

```kotlin
@Composable
fun Advanced() {
    val options = MaskedOptions.custom(
        mask = "Q***************",
        format = "[1][2][3] [4][5][6]-[7][8]-[10][9]",
        onStateChanged = { old, new, event -> /* observe */ }
    )
    val state = rememberMaskedTextFieldState(initialValue = "", maskedOptions = options)

    OutlinedTextField(
        value = state.textFieldValue,
        onValueChange = { state.updateValue(it) },
        label = { Text("Custom") }
    )

    // Values
    val unmasked = state.unmaskedValue
    val formatted = state.getFormattedValue()
}
```

## Masks and Formats

Common patterns:
- Phone: `8 (***) *** **-**`
- Credit card: `**** **** **** ****`
- SSN: `***-**-****`
- Date: `**/**/**` (MM/DD/YY)
- Time: `**:**` (HH:MM)

Use `format` to reorder captured digits in the returned formatted string, e.g. `"[1][2][3] [4][5][6]-[7][8]-[10][9]"`.

## State Machine

Both View and Compose APIs use the same state machine internally. You can observe transitions in Views via `setStateChangeListener(...)` and in Compose via `MaskedOptions(onStateChanged = ...)`.

Key states: `EMPTY`, `PARTIAL`, `COMPLETE`, `INVALID`. Key events: typing, deletion, paste, focus changes, programmatic set, validate.

See `MODERNIZATION.md` and `API_REFACTORING_GUIDE.md` for details.

## Build, Test, Lint

- Build AAR: `./gradlew assembleRelease`
- Publish local snapshot: `./gradlew :masked-edittext:publishToMavenLocal`
- Unit tests (Robolectric + Compose): `./gradlew test`
- Instrumentation/Compose UI tests: `./gradlew connectedAndroidTest`
- Lint: `./gradlew lint` (address or document findings)

## Demo Apps

- Views sample: `demo_app`
- Compose sample: `demo_app_compose`

## Migration from 1.x

- Library is now Kotlin-first; Java interop preserved
- New Compose API (`MaskedTextField`, `MaskedOptions`, state holder)
- `replacementChar`, `deleteChar`, and `maskIconColor` are deprecated/no-op
- Prefer `getFormattedText()` / `state.getFormattedValue()` when using custom `format`
- Cursor handling is policy-driven and more predictable

For a deep dive, see `MODERNIZATION_SUMMARY.md` and `MODERNIZATION.md`.

## License

This project retains the original library’s license; see `LICENSE`.
