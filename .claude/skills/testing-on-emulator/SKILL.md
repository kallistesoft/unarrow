---
name: testing-on-emulator
description: Builds Unarrow, installs it on the Android emulator (or the user's Pixel 10) and tests it by tapping through its screens (home, game, daily, settings), reading screenshots and checking logcat for crashes. Use when asked to test, run, smoke-test, QA or check the app (or a change, screen, layout or bug fix) on the emulator, simulator, phone or a device, to check edge-to-edge or insets on a real screen, or to check a release (R8) build before uploading to Play.
---

> Adapted from BetterHunter's skill of the same name; the scripts are the same with Unarrow's package names.

# Testing Unarrow on the emulator

Scripts are in `.claude/skills/testing-on-emulator/scripts/` (`$S` below). Run them; don't read them first.
Screenshots and notes go in `build/emulator-test/` (git-ignored).

```sh
S=.claude/skills/testing-on-emulator/scripts
ADB=~/Library/Android/sdk/platform-tools/adb       # adb isn't on the PATH
mkdir -p build/emulator-test
```

## Devices

| Device | Serial | How to reach it |
|---|---|---|
| Emulator (default) | `emulator-5554` | `$S/emulator.sh` (AVD `BlockDaily_Pixel_9`, 1080×2424) |
| The user's Pixel 10 (1080×2424, gesture nav) | `<ip>:<port>` | wireless debugging: `$ADB mdns services`, then `$ADB connect <ip:port>` |

When both are connected, every adb call needs a target: `export ANDROID_SERIAL=emulator-5554` (or the phone's
`ip:port`) before running the scripts. The `android` CLI ignores `ANDROID_SERIAL`: pass `--device=emulator-5554` to
every `android layout`, `android input` and `android screen` call, or it refuses to run ("Multiple devices").
With animations on, the home (drifting arrows) and a hinted board never go idle: use `android layout --no-idle` and
tap the printed coordinates with `adb shell input tap`, as `android input tap` waits for idle and reports
"No matching elements". Never run `connectedAndroidTest` against the phone: it uninstalls the app
from it. The phone is only there when the user has wireless debugging on; if `adb connect` fails, use the
emulator and say so.

## Workflow

Copy this checklist and tick it off as you go:

```
Emulator test:
- [ ] 1. Code matches GitHub (git fetch, branch up to date)
- [ ] 2. Emulator ready ($S/emulator.sh), ANDROID_SERIAL set if the phone is connected too
- [ ] 3. App built, installed, launched ($S/install.sh)
- [ ] 4. Flows driven, screenshot read for each screen
- [ ] 5. No crash ($S/check_crash.sh) after each flow
- [ ] 6. Report written
```

**1. Code matches GitHub.** Run `git fetch` then `git status -sb`. If the branch is behind, `git pull --ff-only`. If it
can't fast-forward or has local changes you didn't make, tell the user which code you are testing instead of
guessing.

**2. Emulator.** `$S/emulator.sh` reuses a running emulator or cold-boots AVD `BlockDaily_Pixel_9` and prints its
serial. If it fails twice, stop and tell the user; don't loop.

**3. Install.** Pick the build (the scripts set `JAVA_HOME` to Android Studio's runtime and `ANDROID_HOME` to
`~/Library/Android/sdk`, so Gradle builds locally):

| Goal | Command |
|---|---|
| Test a change (default) | `$S/install.sh debug` |
| From level 1 with no progress (tutorial, first daily) | `$S/install.sh debug --fresh` |
| Before a Play upload, or anything about R8, serialization, reflection | `$S/install.sh release --fresh` |
| Reinstall without rebuilding | add `--no-build` |

Debug is `ca.kallistesoft.unarrow.debug` ("Unarrow Dev", Google's test ads); release is `ca.kallistesoft.unarrow`
(real AdMob units: never tap its ads). A Gradle failure stops here: report the compile error, don't test an old APK.

**4. Drive the app.** One loop per screen:

```sh
android layout --flat 2>/dev/null | python3 -I $S/screen_text.py   # what is on screen, with tap points
android input tap "text=Play" --wait=3000                            # tap by visible text
android screen capture -o build/emulator-test/02-game.png            # then Read the PNG and look at it
```

`screen_text.py` prints each element as `text="…"` or `desc="…"`: tap with that same selector (icon buttons are
content descriptions: `desc=Settings`, `desc=Back`; arrows on the board are `desc=Arrow pointing up` and so on). Use
coordinates (`$ADB shell input tap X Y`) only for elements with neither. When a label appears twice, add
`--match=first`. Labels come from `core/ui/src/main/res/values/strings.xml` (French in `values-fr/`, Quebec
differences in `values-fr-rCA/`). Read every screenshot you take: layout bugs (clipped text, a top bar under the
status bar, a banner under the gesture bar, overlap, untranslated strings) only show up there, not in the layout
dump.

For what to cover, see [reference/test-plan.md](reference/test-plan.md). For back, solving a board, language,
dark mode, font size, rotation and process death, see [reference/driving.md](reference/driving.md).

**5. Crashes.** Run `$S/check_crash.sh` (add `release` for the release build) after each flow. It exits 1 with
the stack trace on a crash, ANR or R8/serialization error. When it fails:

1. Read the stack trace and find the cause in the code
2. Fix it, then `$S/install.sh` again (logcat is cleared on each launch)
3. Repeat the same flow and run `$S/check_crash.sh` again
4. Go on only when it prints `OK`

If you weren't asked to fix bugs, report them instead of changing code.

**6. Report.** Use this shape:

```markdown
## Emulator test: <branch> @ <short sha>, <debug|release>
Device: BlockDaily_Pixel_9 (Android <version>) | Pixel 10

| Flow | Result | Notes |
|---|---|---|
| Home → Play → solve level 1 | ✅ | |
| Daily puzzle | ❌ | Top bar under the status bar (build/emulator-test/05-daily.png) |

Issues: one bullet per problem: screen, steps, expected vs seen, screenshot path.
Not tested: anything skipped and why.
```

Say plainly what failed or wasn't covered. Leave the emulator running at the end.
