# Scoreboard

A touch-first two-team Android scoreboard built with Kotlin and Jetpack Compose.

## Features

- Large landscape-first scoreboard with portrait support
- Swipe up to add a point and swipe down to remove a point
- Two-second gesture hint instead of permanent on-screen instructions
- Long-press either score to enter a non-negative score manually
- Winning teams cannot add points after victory, but can still correct downward
- Non-winning teams remain editable after a winner is detected
- Scores never go below zero or above an enabled hard cap
- Configurable winning score, win-by-two, and hard cap rules
- Hard cap controls are available only when win-by-two is enabled
- Persistent settings through Android DataStore
- Fullscreen immersive display with keep-screen-awake behavior
- Minimal corner icons for reset and settings
- Stable winner presentation with a border and crown indicator
- Long-press team names to rename them with persistent adaptive sizing
- Cutout-safe, background-free reset and settings controls
- Fixed score/header regions and an inset cutout-safe winner border
- Keyboard/gamepad single-press mapping for scoring and reset actions
- Data model placeholders for double-press and long-press mappings
- GitHub Actions debug APK build and artifact upload

The application ID is `com.hsiaower.scoreboard` and should remain unchanged so future APKs update the installed app.

## Build

Use JDK 17 and Android SDK 35:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The local APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Remote input

Open **Settings > Remote Mapping**, tap **Set input** for an action, then press a button on a paired Bluetooth keyboard, keypad, or gamepad. Single presses are implemented. Double and long press are represented in the input model for future implementation.

## Planned features

- Switch sides
- Match/set history with timestamps
- Rename teams
- Change team colours
- Share live scores
- Undo last action
