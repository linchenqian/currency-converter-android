# Design QA

## Reference

- User-provided side-by-side Android split-screen screenshot dated 2026-08-05
- Saved audit image: `/private/tmp/currency-converter-split-screen-audit/01-half-screen-comparison.png`
- The reference app fits two currency rows and all five calculator rows into a half-height window by reducing information density and unused space
- The previous build devoted most of the available height to the conversion panel, leaving only the first calculator row visible

## Implemented correction

- Added three responsive modes: `Regular` at `780dp` and above, `Compact` from `520dp`, and `HalfScreen` below `520dp`
- Removed the split-screen scrolling workaround; conversion and calculator panels now share the current window height
- Half-screen mode limits the conversion panel to approximately 38% of the available height
- Half-screen mode hides Chinese currency names, expression labels, the “约合” label, and the exchange-rate timestamp while retaining flags, currency codes, amounts, and the swap control
- Currency flags, amount typography, swap control, panel padding, and blank space compress together in half-screen mode
- The calculator uses all remaining height, with tighter outer padding, row/column gaps, key radii, operator width, icon sizes, and type sizes
- Full-screen mode retains the approved layout and styling; medium-height windows use an intermediate compact density
- Fixed portrait orientation remains disabled so Android multi-window can resize the activity normally

## Automated verification

- `WindowLayoutPolicyTest` covers all three modes and both responsive thresholds
- All 10 JVM unit tests pass, including three responsive-window policy tests
- Android Lint passes with no blocking findings
- Debug APK assembly succeeds as version `1.0.3` (`versionCode 4`)

## Runtime visual comparison

No Android device or emulator is connected to this environment. The implementation cannot yet be captured at the same split-screen viewport and compared against the reference image. Install the generated APK on the reporting device and repeat the same split-screen configuration to complete the final visual check.

final result: blocked

## Launcher icon follow-up — 2026-08-05

- Reduced only the central globe, arrows, and dollar mark to 80% of the v1.0.3 artwork size
- Measured mark bounds changed from approximately `278 × 298px` to `222 × 239px`
- Preserved the `512 × 512px` canvas, textured navy background, colors, line work, and center alignment
- Regenerated legacy square and round launcher resources for mdpi through xxxhdpi from the updated master
- Updated the README icon preview and adaptive-icon foreground from the same master
- Android Lint and all 19 JVM unit tests pass
- Debug APK assembly succeeds as version `1.0.4` (`versionCode 5`)

launcher icon result: passed

## Equals interaction follow-up — 2026-08-05

- A successful equals action immediately replaces the entered expression with its calculated result
- Results use up to 12 significant digits so repeating decimals remain useful without overflowing the expression line
- Entering a digit after equals still starts a new calculation; entering an operator continues from the result
- Invalid calculations such as division by zero remain unchanged so the user can edit the expression
- Added unit coverage for successful and invalid equals actions

equals interaction result: passed

## Percent precision follow-up — 2026-08-05

- Replaced binary floating-point percentage conversion with exact decimal arithmetic
- Verified the reported `1475 × 668.55` case becomes `1475 × 6.6855` after pressing percent
- Verified small percentages such as `0.1%` remain `0.001` instead of being rounded away
- Added regression coverage for both percentage scenarios
- Android Lint, all 19 JVM unit tests, and the version `1.0.5` debug APK build pass

percent precision result: passed

## App name follow-up — 2026-08-05

- Changed the Android launcher label from `汇率计算器` to `Currency`
- Updated the Gradle root project name and README title/description to match
- Kept the application ID `com.jojo.currencyconverter` unchanged for update compatibility
- Verified the built version `1.0.5` APK reports `application-label:'Currency'`
- Android Lint, all 19 JVM unit tests, and APK assembly pass

app name result: passed

## Automatic update follow-up — 2026-08-05

- Added a silent startup check against the repository's latest public GitHub Release
- Added semantic version comparison, GitHub redirect fallback validation, and five JVM regression tests
- Added update choices for immediate update, remind next launch, and ignore the current release
- Immediate update uses Android DownloadManager with visible progress and background notification support
- Downloaded APKs are checked against the GitHub SHA-256 digest and the current application ID before installation
- Added FileProvider sharing and Android's per-app unknown-source permission flow before opening the system installer
- Verified the public GitHub API rate-limit failure falls back to the accessible `releases/latest` redirect
- Runtime download and installer handoff still require verification on a connected Android device

automatic update result: blocked
