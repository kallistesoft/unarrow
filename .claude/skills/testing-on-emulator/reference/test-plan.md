# What to test

Pick the flows that cover the change you are testing; run the whole smoke test before a release or when asked to
"test the app". Screens are in `app/src/main/kotlin/ca/kallistesoft/unarrow/UnarrowApp.kt` (navigation) and
`feature/*/`; if a label below no longer exists, the screen changed: find the new label in `strings.xml` and test
the screen as it is now.

## Contents
- Smoke test
- Checks for every screen
- Checks for a release build

## Smoke test

Start from `install.sh <type> --fresh`. Name screenshots `build/emulator-test/<nn>-<screen>[-fr|-dark].png`.

| # | Flow | Steps | Expect |
|---|---|---|---|
| 1 | Home | Read the screenshot top to bottom | Title, "Play" with "Level 1", "Daily puzzle" with its number (never below #1), Remove ads, Settings icon |
| 2 | First level | Play | "Level 1", 3 hearts, the tutorial line, the board, Hint and Restart, the banner under them (debug: test ad) |
| 3 | Blocked tap | Tap an arrow whose path is blocked | It shakes, the blocker flashes, one heart empties |
| 4 | Win | Clear the board (see driving.md, "Solving a board") | Win panel: "Solved!", taps, Next level, Share; Next opens level 2 |
| 5 | Lose | Tap blocked arrows until no life is left | "Out of lives", "Watch an ad: +1 life" when a rewarded ad is loaded, "Try again" restarts the board |
| 6 | Hint | Hint (from the starting stock) | One free arrow is highlighted, the hint count drops |
| 7 | Daily | Back home → Daily puzzle → solve it | "Daily #n" title; win panel has Home; home then says it's solved today and the streak shows |
| 8 | Interstitial | Reach level 5+ and solve 3 levels | After the win panel's Next, a test interstitial; never during a puzzle |
| 9 | Settings | `desc=Settings` | Sound, Vibration, Language, Restore purchases, Privacy options (EU only), Rate, version |
| 10 | French | Settings → Language → Français (Canada), then visit 1, 2, 4, 9 | Every string in French; no English left, no clipped text |
| 11 | Dark theme | `cmd uimode night yes`, revisit 1, 2, 4 | Dark palette, readable arrows and panels |
| 12 | Process death | In a game: HOME, `am kill`, relaunch (see driving.md) | Reopens on the game, not home |

Share opens the system share sheet: check it opens, then system back. Don't buy anything in Remove ads or the hint
pack; check the Play purchase sheet opens (or "Purchases are unavailable" on an emulator without Play sign-in).

## Checks for every screen

From the screenshot:
- Edge to edge: nothing under the status bar (clock, icons) or the display cutout, nothing under the gesture bar or
  the 3-button bar; the banner sits right above the navigation bar with no double gap
- Text: nothing clipped or overlapping; French strings are longer
- Win and lose panels sit above the banner and the navigation bar

From `screen_text.py`:
- Every tappable element has a label (text or desc): an unlabelled icon button is an accessibility bug

## Checks for a release build

Run the smoke test on `install.sh release --fresh` before any Play upload. R8 problems show up only here:
- Progress saved in DataStore survives a relaunch, and the back stack survives process death (serialization)
- `check_crash.sh release` after every flow; it also flags `ClassNotFoundException` and `SerializationException`
- Release builds use the real AdMob units: look at the ad, never tap it
