# Repository Guidelines

## Project Structure & Module Organization
- `masked-edittext/` hosts the reusable library; primary sources sit in `src/main/java/com/thrd/maskededittext` with resources under `src/main/res`.
- `app/`, `demo_app/`, and `demo_app_compose/` provide sample clients; keep shared helpers inside the library, not the demos.
- Tests live beside code: JVM specs in `src/test`, Compose instrumentation in `src/androidTest`, and Gradle scripts at the module root.

## Build, Test & Development Commands
- `./gradlew assembleRelease` builds the library AAR for distribution and validates release config.
- `./gradlew :masked-edittext:publishToMavenLocal` exercises the publishing pipeline and produces a local snapshot for integration testing.
- `./gradlew test` runs JVM + Robolectric suites across modules; execute before every push.
- `./gradlew connectedAndroidTest` launches instrumentation and Compose UI tests on a device or emulator.
- `./gradlew lint` runs Android Lint and Compose metrics; address findings or document accepted risk.

## Coding Style & Naming Conventions
- Kotlin is the default language; use 4-space indents, trailing commas where it improves diffs, and prefer expression-bodied functions when clear.
- Classes, composables, and test fixtures use `PascalCase`; methods and variables use `camelCase`; constants stay in `CONSTANT_CASE`.
- Group Compose UI under `ui/` packages, data + masking logic under `core/`, and keep any Java interop isolated and documented.
- Run `./gradlew lint` before committing; avoid suppressing warnings unless a tracking issue exists.

## Testing Guidelines
- Rely on JUnit4 + Robolectric for mask behaviour; mirror production packages in `src/test` for clarity.
- Compose UI assertions belong in `src/androidTest` with `createAndroidComposeRule` utilities.
- Name tests as `functionUnderTest_state_expectedResult` to emphasise behaviour.
- Prioritise coverage around mask configuration, cursor placement, locale handling, and accessibility announcements.

## Commit & Pull Request Guidelines
- Match the concise, imperative history (`Update Kotlin version`, `Remove unused java code`); scope each commit to one concern.
- PR descriptions should outline intent, list touched modules, and reference issues or Jira tickets when applicable.
- Attach emulator logs or screenshots whenever UI or UX changes are visible.
- Confirm `./gradlew lint test` passes locally and note the run in the PR summary before requesting review.
