---
name: unit-testing
description: How to write and run Unarrow's unit tests (JVM + Robolectric, no device) and keep Kover coverage at 90% of lines for every class. Use when adding or changing any code (a ViewModel, a screen, a repository, a worker), writing or fixing a test, when `koverVerifyUnit` or the CI "Coverage" step fails, or when asked to raise coverage. For tests on a device or emulator, see .claude/skills/testing-on-emulator.
---

> Adapted from BetterHunter: the code examples (quiz, exams, hunters) come from that app; the rules are the same in
> Unarrow. Unarrow's own references: `HomeViewModel` and `SettingsViewModel` (Pattern A), `GameViewModel` (a session
> loaded in `onStart`, combined with progress, ads and prices), fakes in `core/testing`.

# Unit tests and coverage in Unarrow

**Rule: every class keeps at least 90% of its lines covered by the unit tests.** `./gradlew :koverVerifyUnit`
(CI's "Coverage" step) fails otherwise. New code comes with its tests in the same change.

Kover (`build-logic/.../KoverConventionPlugin.kt`) measures each module's `unit` variant (debug unit tests, or the
JVM tests of `:core:model`) and merges them at the root, so a class may be covered by another module's tests. Left
out: generated code (Hilt, Dagger, `ComposableSingletons`, `BuildConfig`) and `@Preview` functions. `:core:testing`
and `:smoketest` aren't measured.

## Commands

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME=~/Library/Android/sdk
./gradlew :feature:quiz:testDebugUnitTest --tests '*QuizScreenTest*'   # one module / one class while working
./gradlew testDebugUnitTest :core:model:test                          # every unit test
./gradlew :koverVerifyUnit                                            # the coverage rule (runs the tests it needs)
./gradlew :koverHtmlReportUnit     # build/reports/kover/htmlUnit/index.html: the missed lines, in red
./gradlew :koverXmlReportUnit      # build/reports/kover/reportUnit.xml, for scripts
```

Use `:koverVerifyUnit` (with the colon, the merged report). Without it, Gradle also runs each module's own task,
which has no rule but costs time.

## Reading a failure

```
lines covered percentage for class 'ca.kallistesoft.unarrow.feature.quiz.QuizViewModel$restart$1' is 0.0 …
```

Kover counts **every JVM class**, including the ones Kotlin generates inside yours:

| Class in the report | What it is | How to cover it |
|---|---|---|
| `FooKt` | top-level functions of `Foo.kt` (composables) | render them with every state they branch on |
| `Foo$bar$1` | a `launch { }` / suspend lambda in `bar()` | call `bar()` and let the coroutine run |
| `FooKt$Content$2$1$1$3$1$1` | a click lambda inside a composable | `performClick()` on that button and assert the callback |
| `Foo$Companion` | companion of a `@Serializable` class or with a non-private `val` | serialize it, or make an unused `val` `private` |
| `Foo$DefaultImpls` | default method of an interface | call the default |

Open the HTML report on that class to see the exact lines.

## Where a test goes, and what it uses

| Code | Test | Tools |
|---|---|---|
| `core/model` rules | plain JUnit, `core/model/src/test` | fixtures in `Fixtures.kt` |
| ViewModel | JVM test, `FooViewModelTest` | `MainDispatcherRule`, `runTest`, Turbine or `collecting()`, fakes from `:core:testing` |
| Screen / composable | Robolectric Compose test, `FooScreenTest` | `createComposeRule()`, `UnarrowTheme`, real ViewModel + fakes |
| Preview | screenshot test, `FooScreenshotTest` | `captureScreenshot` (see README, "Screenshot tests") |
| DataStore repository | JVM test on a temp file | `TemporaryFolder` |
| Firebase / Play | through the class's seam | MockK (`FirebaseFeatureFlagsTest`), Play's `FakeAppUpdateManager` (`ForceUpdateScreenTest`) |
| App navigation, Activity, WorkManager | Robolectric in `app/src/test` | `AppRobot`, `TestUnarrowApplication`, `work-testing` |

Shared doubles live in `:core:testing`: `FakeContentRepository`, `FakeUserDataRepository`, `FakeFeatureFlags`,
`TestContent`, `TEST_DAY`, `MainDispatcherRule`, `collecting()`, `ScreenshotDevice`. Add to them rather than writing
a second fake in a module.

### A ViewModel test

The state only loads while collected (.claude/skills/viewmodel-state), so collect it first:

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

private val userData = FakeUserDataRepository()

private fun viewModel(mode: QuizMode, saved: SavedStateHandle = SavedStateHandle()) =
    QuizViewModel(TestContent.pal.id, mode, FakeContentRepository(), userData, saved, FakeFeatureFlags(), Today { TEST_DAY })

@Test
fun `practice answers are recorded right away`() = runTest(mainDispatcherRule.testDispatcher) {
    val vm = viewModel(QuizMode.PRACTICE)
    vm.uiState.test {
        assertEquals(QuizUiState.Loading, awaitItem())
        runCurrent()                                   // the first load
        awaitItem()
        vm.choose(1)
        assertEquals(1, (awaitItem() as QuizUiState.Running).session.currentAnswer)
        runCurrent()
        assertEquals(1, userData.recorded.size)        // what the fake repository received
    }
}
```

With `StandardTestDispatcher`, `runCurrent()` runs pending work and `advanceTimeBy` moves timers (see
`QuizViewModelTest`). Actions that call back (`confirm(onSaved)`) are tested by counting the callbacks, including a
second tap (`PassedExamViewModelTest`).

### A screen test

Drive the real screen with a real ViewModel built on fakes, click every action and assert what happened, not only
that something rendered:

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK], qualifiers = ScreenshotDevice.LIGHT_EN)
class ExamTrainingScreenTest {
    @get:Rule val compose = createComposeRule()
    private val events = mutableListOf<String>()

    @Test
    fun `each exam card starts its quiz`() {
        compose.setContent {
            UnarrowTheme {
                ExamTrainingScreen(onStartQuiz = { id, mode -> events += "$id $mode" }, /* … */
                    viewModel = ExamsViewModel(FakeContentRepository(), FakeUserDataRepository()))
            }
        }
        compose.onNodeWithTag("practice-ICAF").performClick()
        assertEquals(listOf("ICAF ${QuizMode.PRACTICE}"), events)
    }
}
```

- Find nodes by **test tag** first, then by the app's own strings through the module's test `Strings.kt`
  (`string(R.string.back)`), never by a hard-coded English text.
- Content taller than the screen: `performScrollTo()` (or `performScrollToNode` in a lazy list).
- Each branch of the `UiState` (loading, empty, error, done) is a state to render once.

## Rules

- **Test behavior.** Each test name says what the user or the code gets (`` `a wrong answer shows the right one` ``);
  each test asserts a result: a callback, a saved value, a shown text. A test that only executes lines doesn't count.
- **No Kover exclusions for hand-written code, and never lower the threshold.** If a line truly can't run on the JVM,
  add a seam (an `internal` overload taking the dependency, like `ForceUpdateScreen(storePackage, manager)`), keep
  it minimal, and say why in a comment.
- **Dead code is deleted**, not tested: an unreachable branch or getter that drags a class under 90% is a finding.
- Fakes over mocks. Use MockK only for third-party classes without a seam (Firebase).
- No wall clock or real time in assertions: use `TEST_DAY`, the `Today` seam and `runTest` virtual time. If a test
  must depend on the hour, `assumeTrue` it and keep the class above 90% without it (`ReminderWorkerTest`).
- Don't change screenshot references in a coverage change; recording them is a separate, reviewed step.

## Checklist

```
Unit tests:
- [ ] New or changed code has tests in the same change (ViewModel test and/or screen test)
- [ ] Every new click lambda, launch { } and UiState branch runs in a test
- [ ] Tests assert results and are named for the behavior
- [ ] ./gradlew testDebugUnitTest :core:model:test passes
- [ ] ./gradlew :koverVerifyUnit passes (no class under 90%)
- [ ] No new Kover exclusion except for generated code
- [ ] scripts/check-conventions.sh prints "Conventions OK."
```
