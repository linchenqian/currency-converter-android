# Design QA

## Reference

- User-approved calculator-style currency converter design from the current Codex task
- User-approved v18 app icon

## Automated checks

- Native Android debug build: passed
- Expression and currency catalog unit tests: passed
- APK manifest/package inspection: passed
- Responsive amount sizing is implemented using measured available width
- Light and dark palettes follow the Android system theme

## Runtime visual comparison

No Android device or emulator is available in the current environment, so a runtime screenshot could not be captured and compared with the reference at the same viewport. A real-device pass should verify:

- Compact and tall phone layouts
- Very long source and converted values
- Light and dark system themes
- Currency picker search and flag rendering
- Keyboard alignment, operator dividers, and centered swap control

final result: blocked
