# Unarrow: build progress

Claude updates this file and pushes it after every step. Work happens on the `develop` branch; there will be no pull
request until the release.

**Now:** build 34 is in internal testing; the store listing is drafted in three languages; waiting on the AI
declaration and the in-app products (below).
**CI:** ✅ green on every push: conventions, all unit tests, coverage ≥ 90% per class, lint, debug APK, release APK
and bundle (R8). The APKs and the `.aab` are in the run's `unarrow-<n>` artifact.
**Blockers:** none. Local builds work on this Mac (see CLAUDE.md); GitHub Actions stays the reference.

## Steps

| # | Step | Status | Commit |
|---|------|--------|--------|
| 1 | Repo base: `main` seeded, `develop` branch, this file | ✅ | |
| 2 | Gradle skeleton: version catalog, wrapper, build-logic convention plugins, CI workflow | ✅ | 8404762 |
| 3 | `:core:game`: board, arrows, puzzle generator, levels, daily puzzle, tests | ✅ | 8404762 |
| 4 | `:core:designsystem` + `:core:ui`: theme (dark and light), shared strings in en, fr-FR, fr-CA | ✅ | 714661e |
| 5 | `:core:data`: progress (level, streak, hints, wins) on DataStore | ✅ | 714661e |
| 6 | `:feature:game`: game screen, board drawing, taps, lives, hints, win and lose | ✅ | babc667 |
| 7 | `:feature:home`: home screen, Play, Daily, streak | ✅ | babc667 |
| 8 | `:app`: Application, MainActivity, Navigation 3, splash, edge to edge, build types | ✅ | babc667 |
| 9 | `:core:flags`: Firebase Remote Config | ✅ | d31f64a |
| 10 | `:core:update`: force update (Remote Config) + Play in-app updates (immediate and flexible) | ✅ | babc667 |
| 11 | Crashlytics (release builds, once Firebase is set up) | ✅ | babc667 |
| 12 | `:core:ads`: UMP consent, interstitial, rewarded (hint, extra life) | ✅ | d31f64a |
| 13 | `:core:billing`: Remove ads + hint pack (Play Billing 9) | ✅ | 200c036 |
| 14 | `:core:engagement`: share result with the Play link, in-app review prompt | ✅ | babc667 |
| 15 | `:feature:settings`: language, sound, haptics, restore purchases, privacy options | ✅ | babc667 |
| 16 | Languages: en, fr-FR, fr-CA complete, missing-key check | ✅ | 714661e |
| 17 | Polish: slide-out animation, haptics, sounds, launcher icon | ✅ | 31e8ef8 |
| 17b | Endless levels: 9×9 past level 36, fill drawn per level (70–90%), a lighter level every 10 | ✅ | 31e8ef8 |
| 18 | Coverage at 90% (Kover), lint clean, release (R8) build | ✅ | 70ca30f |
| 19 | Store listing texts (en-US, fr-CA, fr-FR) and Data safety notes in `store/` | ✅ | 31e8ef8 |
| 19b | Real AdMob IDs (release), banner under the board, interstitial pacing from Remote Config | ✅ | 112dca4, df6e762, f548391 |
| 19c | `testing-on-emulator` skill (emulator and the Pixel 10 over wireless adb) | ✅ | 075f8e9 |
| 19d | Edge to edge: game and settings top bars below the status bar; banner above the gesture bar | ✅ | 2589788 |
| 19e | Daily number never below #1 before the first daily | ✅ | ef78ca2 |
| 19f | Harder puzzles: few free arrows, long chains, faster curve (seed salts bumped), `DifficultyTest` | ✅ | 0778542 |
| 19g | Timer and stars (best per level, best daily), stars and time in shares | ✅ | f433acb |
| 19h | Sliding, rotating and locked arrows; solver (perfect line, traps, hints); curve and daily with the mechanics | ✅ | 6c923fc |
| 19i | Nocturne redesign (studio palette, Manrope / JetBrains Mono, cards and chips), theme choice, Kallistesoft icon and splash, screenshot tests; English/French only | ✅ | 9c7b059, a9e4b42, 2e637d6, 1599e94 |
| 19j | Landing home like the Neon games, in Nocturne: gradient and drifting arrows, glowing two-tone title and Play pill, records, special daily card (solved title fixed), how to play, sound and site footer | ✅ | 9e08bf3 |
| 19k | History of levels and dailies (best stars and time, migration of earlier data), replay after a rewarded ad; home records in equal columns | ✅ | 7802670, ece453b |
| 19l | Fancier board: pieces on a tray, direction colours, richer locks/keys/rotation, exit trails, win ripple | ✅ | 77c2334 |
| 20 | Release: store graphics ✅, listing translations ✅, internal testing ✅, products, pull request `develop` → `main` | ⏳ | |

✅ done · ⏳ in progress · ⬜ to do · ❌ blocked

## Needs you at release

Done in the consoles:

- ✅ **GitHub secrets:** `GOOGLE_SERVICES_JSON`, `UA_KEYSTORE_BASE64`, `UA_KEYSTORE_PASSWORD`, `UA_KEY_ALIAS`,
  `UA_KEY_PASSWORD` (the upload key is kept outside the repo).
- ✅ **Firebase:** project `unarrow-d140e` (contact@kallistesoft.ca), both Android apps registered
  (`ca.kallistesoft.unarrow`, `ca.kallistesoft.unarrow.debug`). Remote Config v1 published with the 7 parameters
  below. Crashlytics turns on by itself with the first crash report: nothing to switch on.

  | Parameter | Type | Default | What it does |
  |---|---|---|---|
  | `min_version_code` | number | 0 | force update below this versionCode (`min_version_code_debug` for debug) |
  | `ads_enabled` | boolean | true | every ad (interstitial, rewarded, banner) |
  | `rewarded_ads` | boolean | true | "Watch an ad" for a hint or an extra life |
  | `review_prompt` | boolean | true | asking for a Play rating |
  | `interstitial_first_level` | number | 5 | first level after which interstitials may show (below 1: default) |
  | `interstitial_every` | number | 3 | one interstitial every this many solved puzzles (below 1: default) |
  | `interstitial_cap_seconds` | number | 180 | at most one interstitial every this many seconds (below 0: default) |
- ✅ **AdMob:** app "Unarrow" and its units (in `app/build.gradle.kts`, release builds only): app
  `ca-app-pub-7557144664039757~7253132965`, interstitial `…/5991936465`, rewarded `…/9739609786`, banner
  `…/4327501007`. Ad content rating T, sensitive categories blocked as for Block Daily, the EU and US-states consent
  messages include Unarrow. Keep AdMob's interstitial frequency cap no tighter than `interstitial_cap_seconds`.
- ✅ **Play Console:** app "Unarrow: Daily Arrow Puzzle" created; privacy policy URL, ads, app access, government,
  financial, health, advertising ID, target audience 13+, data safety and the IARC content rating (Everyone/ESRB,
  Brazil 14+ because of in-app purchases) are filled in.
- ✅ **Privacy policy:** `https://kallistesoft.ca/en/privacy/unarrow/` (FR: `/confidentialite/unarrow/`).

Done on 2026-10-30:

- ✅ **Internal testing:** build 34 (1.0.0) is live for the "KallisteSoft team" list; opt-in link
  https://play.google.com/apps/internaltest/4701454878350786133
- ✅ **Store settings:** category Puzzle, contact contact@kallistesoft.ca, website https://kallistesoft.ca.
- ✅ **Listings (saved as a draft):** en-US, fr-CA and fr-FR texts, 512 px icon, feature graphics and 5 phone screenshots
  per language in both themes, from `tools/make_store_graphics.py`.

Still open:

1. **AI declaration:** the listing's review step asks whether to label AI-generated assets; the owner answers it.
2. **In-app products:** `remove_ads` (one-time, 3.99 CAD) and `hints_10` (consumable, 0.99 CAD).
3. **Before production:** test the difficulty with a few people, reword "calm" and "relaxing" if needed, pull request
   `develop` → `main`, then link the store on the existing AdMob entry `~7253132965` at launch.

## Log

- 2026-10-25: plan approved; repo seeded.
- 2026-10-25: engine, storage, strings (3 languages), ads, billing, force update, review, sharing, all screens and
  the app written; CI is the compiler (this environment can't download the Android SDK).
- 2026-10-25: CI green on tests, coverage and lint (run #9); endless level curve; sounds; store texts.
- 2026-10-25: CI fully green (run #10), release bundle built with R8.
- 2026-10-25: privacy policy live on kallistesoft.ca (kallistesoft-site PR #5, merged).
- 2026-10-25: Firebase project and AdMob app created (console); release builds use the real AdMob units; a banner
  sits under the board while ads are allowed (gone after Remove ads); interstitial pacing (`interstitial_first_level`,
  `interstitial_every`, `interstitial_cap_seconds`) comes from Remote Config, with 5, 3 and 180 s as defaults.
- 2026-10-25: after a first test on the Pixel 10: `testing-on-emulator` skill; the game and settings top bars no
  longer sit under the status bar; "Daily #-6" before the first daily fixed (#1); much harder puzzles (about one arrow
  in seven free at the start instead of one in three, 15–25 rounds on a 9 × 9 instead of 5–8; every level and daily
  board changed, seed salts bumped); a clock and 1–3 stars per puzzle, best results saved and shared. Console steps
  done: secrets, Remote Config, Play Console app and declarations.
- 2026-10-25: "not hard enough, no reflection needed": a blocked arrow now slides up to its blocker and costs a life
  (a careless tap can trap you), rotating arrows turn after every move, locked arrows wait for their key. A solver
  (exits only, memoised) checks every board has a perfect line, picks boards with traps, measures difficulty and
  gives the hints. Locks from level 10, rotating arrows from 20, the daily mixes all three; tips the first time each
  appears; boards are made off the main thread. Seed salts bumped again: every board changed.
- 2026-10-25: a solved daily can no longer be reopened from its card (it could be replayed for free); "Play again
  (ad)" replays it after a rewarded ad. The streak never counts a day twice; a replay can only improve the best daily.
- 2026-10-25: the banner strip runs to the bottom edge, behind the gesture bar; the ad itself stays above it.
- 2026-10-25: redesign in the studio's "Nocturne" look (kallistesoft.ca): its palette in light and dark, Manrope with
  extra-bold headings and JetBrains Mono labels (bundled, SIL OFL), site-style buttons, cards, chips and pills; a
  Theme choice in Settings (system, light, dark); a new launcher icon and splash in the Kallistesoft game style
  (board corner with K and S, the sun and the waves, on navy), drawn by `tools/make_icon.py`; screenshot references
  for every screen in both themes. Settings now offers English or French only; French uses the Quebec wording on a
  Canadian phone and France's elsewhere.
- 2026-10-25: the home is now a landing screen in the family of the Neon games, in Nocturne colours: a gradient with a
  few arrows drifting behind (still when the system's animations are off), "UN" + "ARROW" glowing in two colours with
  a staggered entrance, a glowing Play pill, records (level, stars earned, best daily streak), a daily card with an
  accent-to-amber edge, the streak and today's result (its title no longer greys out once solved), a "how to play"
  card with the board's glyphs, and a footer with the sound toggle and kallistesoft.ca.
- 2026-10-25: a History screen (from home's history button or its records): every solved level and daily with its
  best stars and time; any of them can be played again after a rewarded ad, which only improves that puzzle's best
  (never the current level, the streak or the last solved day) and has no interstitial. The progress now keeps each
  level's and each daily's best result; earlier data moved over (level stars with an unknown time, the last solved
  daily). Home records sit in three equal columns.
- 2026-10-25: the board looks like game pieces on a tray: raised tiles with depth, holes for empty cells, arrows
  coloured by direction (blue, green, amber, coral), locked tiles in their lock's colour with a large padlock, glowing
  keys, a spinning ring on rotating arrows; exits leave a trail and a burst, a win ripples before the stars pop in.
  Still when the system's animations are off.
- 2026-10-30: first internal-testing release (build 34); category, contact details and the three listings filled in;
  store graphics generated from the screenshot references; fr-CA title shortened to 27 characters.
