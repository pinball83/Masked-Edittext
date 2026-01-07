# Repository Guidelines

## Project Structure & Module Organization
- `masked-edittext/` contains the library module.
  - Core masking and state: `src/main/java/com/github/pinball83/maskededittext` (e.g., `MaskFormatter.kt`, `InputStateMachine.kt`, `CursorPolicyController.kt`).
  - Jetpack Compose API: `src/main/java/com/github/pinball83/maskededittext/compose`.
  - Android resources: `src/main/res`.
- `demo_app/` and `demo_app_compose/` are sample apps. Keep shared helpers in the library, not in demos.
- Tests live beside code: JVM specs under `src/test`, Compose instrumentation under `src/androidTest`. Gradle scripts stay at module roots.

## Build, Test & Development Commands
- `./gradlew assembleRelease` builds the library AAR and validates release config.
- `./gradlew :masked-edittext:publishToMavenLocal` exercises the publishing pipeline and produces a local snapshot for integration testing.
- `./gradlew test` runs JVM + Robolectric suites across modules; execute before every push.
- `./gradlew connectedAndroidTest` launches instrumentation and Compose UI tests on a device or emulator.
- `./gradlew lint` runs Android Lint and Compose metrics; address findings or document accepted risk.

## Coding Style & Naming Conventions
- Kotlin is the default language; use 4-space indents, trailing commas where they improve diffs, and prefer expression-bodied functions when clear.
- Classes, composables, and test fixtures use `PascalCase`; methods and variables use `camelCase`; constants stay in `CONSTANT_CASE`.
- Compose API lives under the `compose/` package; shared mask logic stays in the root package. Keep any Java interop isolated and documented.
- Run `./gradlew lint` before committing; avoid suppressing warnings unless a tracking issue exists.

## Testing Guidelines
- Use JUnit4 + Robolectric for mask behaviour; mirror production packages in `src/test` for clarity.
- Compose UI assertions belong in `src/androidTest` with `createAndroidComposeRule` utilities.
- Name tests as `functionUnderTest_state_expectedResult` to emphasise behaviour.
- Prioritise coverage around mask configuration, cursor placement, locale handling, and accessibility announcements.

## Refactoring Summary & Docs
- Kotlin-first rewrite with Java interop preserved.
- Unified mask core shared by View and Compose.
- New Compose API with grouped options (`MaskedOptions`, `MaskedVisualOptions`, `MaskedInputOptions`).
- Deprecated/no-op XML attributes kept for compatibility: `replacementChar`, `deleteChar`, `maskIconColor`.
- See `MODERNIZATION.md`, `MODERNIZATION_SUMMARY.md`, and `API_REFACTORING_GUIDE.md` for details and migration guidance.

## Commit & Pull Request Guidelines
- Match concise, imperative history (`Update Kotlin version`, `Remove unused java code`); scope each commit to one concern.
- PR descriptions should outline intent, list touched modules, and reference issues or Jira tickets when applicable.
- Attach emulator logs or screenshots whenever UI or UX changes are visible.
- Confirm `./gradlew lint test` passes locally and note the run in the PR summary before requesting review.
