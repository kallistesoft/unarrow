# Driving the emulator

`ADB=~/Library/Android/sdk/platform-tools/adb` (adb isn't on the PATH). `PKG` is `ca.kallistesoft.unarrow.debug`,
or `ca.kallistesoft.unarrow` for the release build. With the phone connected too, `export ANDROID_SERIAL=…` first.

## Contents
- Reading the screen
- Input
- Solving a board
- Device settings (language, dark mode, font size, rotation)
- App state (reset, process death)
- The user's phone
- When things go wrong

## Reading the screen

```sh
android layout --flat 2>/dev/null | python3 -I $S/screen_text.py    # compact list: selector, actions, @x,y
android layout --flat --full -o build/emulator-test/layout.json      # everything, including hidden nodes
android screen capture -o build/emulator-test/<nn>-<screen>.png
android screen capture --annotate -o build/emulator-test/a.png       # numbered boxes, for elements without text
android screen resolve ...                                           # turns "#3" from an annotated shot into x,y
```

In Compose, a clickable card has no text; its label is a separate node inside it. Tapping the label taps the card.

## Input

```sh
android input tap "text=Play" --wait=3000
android input tap "desc=Settings"                        # icon button
android input tap "desc=Arrow pointing up" --match=first # an arrow on the board
android input scroll "scrollable=true" --dir=down --until="text=Restore purchases"
$ADB shell input keyevent KEYCODE_BACK                   # system back (NavDisplay onBack)
$ADB shell input tap 540 794                             # last resort: coordinates from screen_text.py
```

After a tap that changes screens, wait for the new screen with `--wait` on the next tap, or `sleep 1` before
`android layout`; Compose animations otherwise show the old screen.

## Solving a board

Each arrow is a node `desc="Arrow pointing <direction>"` with its tap point. An arrow only leaves when nothing is in
its way; a blocked tap costs one of the 3 lives. To win without guessing, read the screenshot, find an arrow with a
clear path to the edge, tap it by coordinates, and repeat. To test the lose panel, tap a blocked arrow 3 times.
Interstitials only come after a win from level 5 on (debug: Google's test ads, close them with system back or the
ad's close button).

## Device settings

| Check | Command | Undo |
|---|---|---|
| French UI | `$ADB shell cmd locale set-app-locales $PKG --locales fr-CA` (or `fr-FR`) | `--locales en-CA` |
| Dark theme | `$ADB shell cmd uimode night yes` | `night no` |
| Large text | `$ADB shell settings put system font_scale 1.3` | `1.0` |
| Landscape | `$ADB shell settings put system user_rotation 1` (with `accelerometer_rotation 0`) | `user_rotation 0` |
| 3-button navigation | `$ADB shell cmd overlay enable com.android.internal.systemui.navbar.threebutton` | `… navbar.gestural` |

The app also has its own language picker in Settings; test it once by tapping, use the command for the rest.
Undo every setting before finishing so the next test starts from the default.

## App state

```sh
$ADB shell pm clear $PKG                       # wipe progress: level 1, no streak, starting hints
# Process death: the back stack (Serializable NavKeys) must survive it.
$ADB shell input keyevent KEYCODE_HOME
sleep 2                                        # am kill only kills a background app; right after HOME it does nothing
$ADB shell am kill $PKG
$ADB shell pidof $PKG || echo killed           # must print "killed", or the test proves nothing
$ADB shell am start -n $PKG/ca.kallistesoft.unarrow.MainActivity    # should reopen on the same screen
```

## The user's phone

A Pixel 10 (1080×2424, gesture navigation), reached over wireless debugging when the user has it on:

```sh
$ADB mdns services                              # lists adb-tls-connect services with ip:port
$ADB connect 192.168.1.70:46049                 # the ip:port printed above (the port changes)
export ANDROID_SERIAL=192.168.1.70:46049
```

Use it for real-device checks (insets, notch, gesture bar, performance). Don't `pm clear` it or install a release
build over the Play version without asking: the user's own progress is on it.

## When things go wrong

| Symptom | Fix |
|---|---|
| `more than one device/emulator` | Set `ANDROID_SERIAL` (see above) |
| `adb shell` or `android layout` hangs | Run `$S/emulator.sh` again: it detects an unresponsive emulator and cold-boots it |
| "No app window on screen" | App crashed, an ad is open, or the app is in background: run `$S/check_crash.sh`, then relaunch with `install.sh --no-build` |
| "No matching elements" | Label is a content description (`desc=…`), is off screen (scroll to it), or the screen is in French |
| Tap succeeds but screen doesn't change | Element disabled (Restart once the puzzle is over), or a dialog or ad is on top: dump the layout |
| Debug app opens on "Update required" (`testTag` `force-update`) | Firebase Remote Config's `min_version_code_debug` is greater than the local build's `versionCode` (1): set it back in the Firebase console, or build with `-PversionCode=<n>` |
| `INSTALL_FAILED_*` | `install.sh` already handles signature mismatch; for anything else show the message to the user |
| `android` CLI missing | `brew install --cask android-commandlinetools`, or use `$ADB shell uiautomator dump` and `$ADB exec-out screencap -p > shot.png` |
