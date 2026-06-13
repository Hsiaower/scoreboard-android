# Scoreboard

A landscape-first volleyball scoreboard built with Kotlin and Jetpack Compose.

## Features

- Landscape-only dark match dashboard with blue/red score cards
- Tap a score card to add a point; swipe down to subtract
- Long-press names, scores, sets, and timeout counts to edit them
- Persistent scores, sets, timeouts, team names, side assignment, and settings
- New match, clear score, undo, redo, and switch-sides controls
- Basic timeout countdown timer with configurable duration
- Configurable winning score, win-by-two, hard cap, sets to win, and timeouts
- Hard cap controls are available only when win-by-two is enabled
- Winner and match-winner indicators without blocking score corrections
- Three-page first-launch tutorial with a settings shortcut to show it again
- Fullscreen immersive display with keep-screen-awake behavior
- Keyboard/gamepad single-press mapping for scoring and reset actions
- Data model placeholders for double-press and long-press mappings
- Rotation setup button placeholder
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

## Foundation-only features

- Player rotation setup
- Double-press and long-press remote mappings
