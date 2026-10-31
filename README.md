# Unarrow: Daily Arrow Puzzle

Tap the arrows out. One new puzzle every day.

An Android puzzle game by Kallistesoft: endless generated levels and a daily puzzle with a streak, in English and
French (France and Canada). No account: progress stays on the device. Build progress: [PROGRESS.md](PROGRESS.md).

## The game

Each arrow on the grid leaves the board when tapped, if nothing stands between it and the edge it points to.
Otherwise it slides up to the first arrow in its way and costs one of three lives: a careless tap can leave the board
in a dead end. Arrows marked ↻ turn a quarter clockwise after every move; locked arrows (padlock, at most three
colours told apart by shape too) can't move until the key arrow of their colour has left.

Puzzles are generated in reverse (`core/game`, `PuzzleGenerator`), so every one has a perfect line (exits only, no
life lost), and the same seed always gives the same board: level *n* and the daily puzzle of a given day are identical
on every device and in every version (`SeededRandom` is SplitMix64, never `kotlin.random`). A `Solver` checks
boards, picks the ones with traps, and gives the hints (the next move of a perfect line). Mechanics come one at a
time: traps from level 4, locks from 10, rotating arrows from 20; `DifficultyTest` guards the curve.

## Modules

| Module | What |
|---|---|
| `:app` | Application, MainActivity (splash, edge to edge, ads consent, force update gate), Navigation 3 |
| `:feature:game` | The game screen: board, lives, hints, win and lose, sharing |
| `:feature:home` | Home: play, daily puzzle and streak, Remove ads |
| `:feature:history` | Every solved level and daily with its best stars and time; replay one after a rewarded ad |
| `:feature:settings` | Sound, vibration, language, restore purchases, privacy options, rating |
| `:core:game` | Pure Kotlin rules, generator, level curve, daily puzzle |
| `:core:data` | Progress on DataStore (level, streak, hints, wins, settings) |
| `:core:ads` | AdMob: UMP consent, interstitial every 3 puzzles from level 5 (Remote Config), rewarded ads, banner under the board |
| `:core:billing` | Play Billing 9: `remove_ads` (one-time), `hints_10` (consumable) |
| `:core:engagement` | Play In-App Review, share sheet |
| `:core:flags` | Firebase Remote Config: `ads_enabled`, `rewarded_ads`, `review_prompt`; interstitial pacing numbers |
| `:core:update` | Force update (`min_version_code`), Play immediate and flexible in-app updates |
| `:core:ui` | Every string (values, values-fr, values-fr-rCA) |
| `:core:designsystem` | Theme (dark and light), buttons, spacing |
| `:core:common` | The resumed activity, for ads, billing and sharing |
| `:core:testing` | Fakes and test rules |

## Build

```sh
./gradlew :app:assembleDebug           # Unarrow Dev, ca.kallistesoft.unarrow.debug
./gradlew :core:game:test testDebugUnitTest :koverVerifyUnit   # tests and 90% line coverage per class
./gradlew :app:bundleRelease           # R8, signed when the keystore secrets are set
```

CI (`.github/workflows/android.yml`) runs the conventions check, the unit tests, coverage, lint, and builds the debug
APK, the release APK and the release bundle on every push.

### Firebase and Crashlytics

Applied only when `app/google-services.json` exists (CI writes it from the `GOOGLE_SERVICES_JSON` secret). Without
it the app runs with default flags, no forced update and no crash reports. Crashlytics collects from release builds
only.

### Force update

Set the Remote Config parameter `min_version_code` (`min_version_code_debug` for debug builds) to a versionCode:
older installs show a blocking "Update required" screen (Play's immediate update, or the Store listing).

### Ads and purchases

Until the AdMob app exists, every build uses Google's test ad units (`app/build.gradle.kts`, `admobRelease`). The
products `remove_ads` and `hints_10` must exist in Play Console; until then nothing is shown for sale.

## Credits

- Look: Kallistesoft's "Nocturne" theme (kallistesoft.ca), shared by the studio's apps.
- Fonts, bundled in `core/designsystem/src/main/res/font`: [Manrope](https://github.com/googlefonts/manrope) and
  [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), both under the SIL Open Font License 1.1 (licence texts
  in `core/designsystem/src/main/font-licenses`).
