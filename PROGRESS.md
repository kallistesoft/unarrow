# Unarrow: build progress

Claude updates this file and pushes it after every step. Work happens on the `develop` branch; there will be no pull
request until the release.

**Now:** getting CI green on the full app (first full build of every module).
**CI:** engine, design system, strings and storage green; ads, billing and app builds in progress.
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
| 6 | `:feature:game`: game screen, board drawing, taps, lives, hints, win and lose | ⏳ CI | babc667 |
| 7 | `:feature:home`: home screen, Play, Daily, streak | ⏳ CI | babc667 |
| 8 | `:app`: Application, MainActivity, Navigation 3, splash, edge to edge, build types | ⏳ CI | babc667 |
| 9 | `:core:flags`: Firebase Remote Config | ⏳ CI | d31f64a |
| 10 | `:core:update`: force update (Remote Config) + Play in-app updates (immediate and flexible) | ⏳ CI | babc667 |
| 11 | Crashlytics (release builds, once Firebase is set up) | ⏳ CI | babc667 |
| 12 | `:core:ads`: UMP consent, interstitial, rewarded (hint, extra life) | ⏳ CI | d31f64a |
| 13 | `:core:billing`: Remove ads + hint pack (Play Billing 9) | ⏳ CI | 200c036 |
| 14 | `:core:engagement`: share result with the Play link, in-app review prompt | ⏳ CI | babc667 |
| 15 | `:feature:settings`: language, sound, haptics, restore purchases, privacy options | ⏳ CI | babc667 |
| 16 | Languages: en, fr-FR, fr-CA complete, missing-key check | ⬜ | |
| 17 | Polish: slide-out animation, haptics, sounds, launcher icon | ⬜ | |
| 18 | Coverage at 90% (Kover), lint clean, release (R8) build | ⬜ | |
| 19 | Store listing texts (en-US, fr-CA, fr-FR) in `store/` | ⬜ | |
| 20 | Release: pull request `develop` → `main` | ⬜ | |

✅ done · ⏳ in progress · ⬜ to do · ❌ blocked

## Needs you at release

Claude can't sign in to Google's consoles. Until these steps are done, the app runs with Google's test ads, without
Firebase (no force update or Crashlytics), and with no purchases shown.

1. **Firebase** (contact@kallistesoft.ca):
   - add Android apps `ca.kallistesoft.unarrow` and `ca.kallistesoft.unarrow.debug` to a new project "Unarrow";
   - download `google-services.json` and save it as the GitHub secret `GOOGLE_SERVICES_JSON`;
   - turn on Crashlytics;
   - in Remote Config, add `min_version_code` (number, 0) and `ads_enabled` (boolean, true).
2. **AdMob:** add the Android app "Unarrow" and create two ad units: an interstitial ("Level finished") and a rewarded
   ad ("Hint or extra life"). Send the three IDs (app ID and the two units) and Claude puts them in
   `app/build.gradle.kts`.
3. **Play Console:** create the app "Unarrow: Daily Arrow Puzzle" (`ca.kallistesoft.unarrow`, free, game > puzzle), and
   add these in-app products: `remove_ads` (one-time, $3.99) and `hints_10` (consumable, $0.99).
4. **Signing:** create an upload keystore and add the secrets `UA_KEYSTORE_BASE64`, `UA_KEYSTORE_PASSWORD`,
   `UA_KEY_ALIAS` and `UA_KEY_PASSWORD` to the GitHub repo.

## Log

- 2026-10-25: plan approved; repo seeded.
- 2026-10-25: engine, storage, strings (3 languages), ads, billing, force update, review, sharing, all screens and
  the app written; CI is the compiler (this environment can't download the Android SDK).
