# Unarrow: build progress

Claude updates this file and pushes it after every step. Work happens on the `develop` branch; there will be no pull
request until the release.

**Now:** the app is built; waiting on the console steps below for the release.
**CI:** ✅ green (run #10): conventions, all unit tests, coverage ≥ 90% per class, lint, debug APK, release APK and
bundle (R8). The APKs and the `.aab` are in the run's `unarrow-<n>` artifact.
**Blockers:** none. This build environment can't download the Android SDK, so the code is compiled and tested only by
GitHub Actions, on every push.

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
| 20 | Release: Firebase config secret, Play products, signed bundle, pull request `develop` → `main` | ⬜ waits on you | |

✅ done · ⏳ in progress · ⬜ to do · ❌ blocked

## Needs you at release

Claude can't sign in to Google's consoles. Until the remaining steps are done, CI builds without Firebase (no remote
values, force update or Crashlytics) and the app shows no purchases.

1. **Firebase:** ✅ project `unarrow-d140e` (contact@kallistesoft.ca) with both Android apps registered
   (`ca.kallistesoft.unarrow`, `ca.kallistesoft.unarrow.debug`). Still to do:
   - download `google-services.json` and save it as the GitHub secret `GOOGLE_SERVICES_JSON`;
   - turn on Crashlytics;
   - in Remote Config, add these parameters (the app uses the default until the console has a value):

     | Parameter | Type | Default | What it does |
     |---|---|---|---|
     | `min_version_code` | number | 0 | force update below this versionCode (`min_version_code_debug` for debug) |
     | `ads_enabled` | boolean | true | every ad (interstitial, rewarded, banner) |
     | `rewarded_ads` | boolean | true | "Watch an ad" for a hint or an extra life |
     | `review_prompt` | boolean | true | asking for a Play rating |
     | `interstitial_first_level` | number | 5 | first level after which interstitials may show (below 1: default) |
     | `interstitial_every` | number | 3 | one interstitial every this many solved puzzles (below 1: default) |
     | `interstitial_cap_seconds` | number | 180 | at most one interstitial every this many seconds (below 0: default) |
2. **AdMob:** ✅ app "Unarrow" and its units (in `app/build.gradle.kts`, release builds only): app
   `ca-app-pub-7557144664039757~7253132965`, interstitial `…/5991936465`, rewarded `…/9739609786`, banner
   `…/4327501007`. Ad content rating T, sensitive categories blocked as for Block Daily, and the EU and US-states
   consent messages include Unarrow. Keep AdMob's interstitial frequency cap no tighter than
   `interstitial_cap_seconds`.
3. **Play Console:** create the app "Unarrow: Daily Arrow Puzzle" (`ca.kallistesoft.unarrow`, free, game > puzzle), and
   add these in-app products: `remove_ads` (one-time, $3.99) and `hints_10` (consumable, $0.99).
4. **Privacy policy:** ✅ live at `https://kallistesoft.ca/en/privacy/unarrow/` (FR: `/confidentialite/unarrow/`); paste
   the URL in Play Console (App content › Privacy policy).
5. **Signing:** create an upload keystore and add the secrets `UA_KEYSTORE_BASE64`, `UA_KEYSTORE_PASSWORD`,
   `UA_KEY_ALIAS` and `UA_KEY_PASSWORD` to the GitHub repo.

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
