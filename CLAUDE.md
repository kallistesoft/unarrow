# Notes for Claude

- Progress is tracked in `PROGRESS.md`: update it and push it with every step.
- Work on `develop`; no pull request until the release.
- This cloud environment can't download the Android SDK: GitHub Actions is the compiler and test runner. Push, then
  read the run with the GitHub MCP tools (`actions_list`, `get_job_logs`) and fix until green.
- Never ask the user for console values (AdMob, Firebase, Play Console): the app works without them; list what's
  needed under "Needs you at release" in `PROGRESS.md`.

## Skills

Repo skills live in `.claude/skills/`. Read the skill's `SKILL.md` and follow it when the task matches:

- [viewmodel-state](.claude/skills/viewmodel-state/SKILL.md): ViewModels build their state without `init` (`combine` +
  `stateIn`, or `asStateFlow().onStart { }` + `stateIn`), enforced by `scripts/check-conventions.sh`
- [unit-testing](.claude/skills/unit-testing/SKILL.md): JVM and Robolectric Compose tests, Kover 90% of lines for every
  class (`./gradlew :koverVerifyUnit`)
- Android skills (Google): adaptive, edge-to-edge, navigation-3, jetpack-compose-performance, play-billing-library-
  version-upgrade, play-policy-insights, r8-analyzer, android-intent-security, android-permissions-security,
  anr-audit-and-fix, styles, testing-setup

## Conventions

- Strings only in `core/ui/src/main/res/values*/strings.xml`: English in `values`, France in `values-fr`, and only the
  Quebec differences in `values-fr-rCA` (`TranslationsTest` checks it).
- No dp literals outside `:core:designsystem` (use `Spacing` and `Sizes`), no `BuildConfig.DEBUG` branches
  (`src/debug` and `src/release`).
- Levels and daily puzzles must never change for a given seed: don't touch `SeededRandom` or the generator's order of
  random draws without bumping every seed salt on purpose.
