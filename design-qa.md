# Design QA

## Reference

- User-provided Android split-screen screenshot dated 2026-07-31
- Target app window is approximately half of the device height
- Visible defect: the fixed-height conversion panel leaves too little height for the calculator, compressing five keypad rows into thin lines

## Implemented correction

- Windows below `780dp` height now use a compact, vertically scrollable layout
- The compact conversion panel scales between `270dp` and `340dp`
- Currency selectors, amount text, spacing, and the swap control scale down together
- The calculator keeps a fixed usable height instead of accepting destructive parent compression
- Normal-height windows retain the existing approved layout
- Fixed portrait orientation was removed so Android multi-window can resize the activity normally

## Automated verification

- `WindowLayoutPolicyTest` covers split-screen and regular-height thresholds
- All 9 JVM unit tests pass
- Android Lint passes
- Debug APK builds successfully as version `1.0.1` (`versionCode 2`)

## Runtime visual comparison

No Android device or emulator is connected to this environment. The corrected app therefore cannot be captured at the same split-screen viewport for visual comparison yet. Install the generated APK on the reporting device and repeat the same split-screen configuration to complete this check.

final result: blocked
