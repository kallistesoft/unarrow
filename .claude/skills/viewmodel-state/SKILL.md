---
name: viewmodel-state
description: How Unarrow ViewModels build their UI state - never an init block; state derived with combine/map + stateIn, or loaded in _uiState.asStateFlow().onStart { } + stateIn. Use when writing, changing, reviewing or testing any ViewModel (a new screen, a new field in a UiState, loading content or progress, saved state, a ViewModel test), or when scripts/check-conventions.sh reports "no init in a ViewModel".
---

> Adapted from BetterHunter: the code examples (quiz, exams, hunters) come from that app; the rules are the same in
> Unarrow. Unarrow's own references: `HomeViewModel` and `SettingsViewModel` (Pattern A), `GameViewModel` (a session
> loaded in `onStart`, combined with progress, ads and prices), fakes in `core/testing`.

# ViewModel state in Unarrow

**Rule: a ViewModel has no `init { }` block.** Nothing starts when the ViewModel is created; its state starts
when the screen collects `uiState`, and is shared `WhileSubscribed(5_000)`. `scripts/check-conventions.sh` (run by
CI on every push) fails on an `init` in any class extending `ViewModel()`.

**Why:** work in `init` runs even if no screen ever shows (previews, tests, a ViewModel created and discarded by
navigation), races with the first collection, can't be restarted, and hides the data flow in side effects. A state
derived from flows is always current (progress, flags and content changes show at once) and is tested by collecting
it, like the screen does.

## Choose the pattern

```
Is the state a function of repository flows (progress, content, flags) + the user's in-screen choices?
├─ yes → Pattern A: combine (or map) + stateIn            ExamsViewModel, HomeViewModel, ProfileEditViewModel
└─ no, a one-time setup has to happen (draw a quiz, restore a run, compute "today") and actions change the state
   imperatively afterwards → Pattern B: _uiState.asStateFlow().onStart { } + stateIn       QuizViewModel
```

Prefer A. Use B only for state that isn't a pure function of other flows.

### Pattern A: derive with combine

```kotlin
private val draft = MutableStateFlow(Draft(/* restored from savedState */))   // the user's unsaved choices

val uiState: StateFlow<ProfileEditUiState> = combine(userData.progress, draft) { saved, pick ->
    ProfileEditUiState(field, hunterType = pick.hunterType ?: saved.hunterType, /* … */ loading = false)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileEditUiState(field))   // loading = true

fun chooseHunterType(type: HunterType) = draft.update { it.copy(hunterType = type) }
```

- In-screen choices live in their own small `MutableStateFlow` (mirrored to `SavedStateHandle` when they must
  survive process death), never copied into the UI state by hand.
- Writes go to the repository (`viewModelScope.launch { userData.setX(…) }`); the combined state follows by itself.
- The `stateIn` initial value is the loading state (`loading = true` or a `Loading` object).

### Pattern B: load once in onStart

```kotlin
private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)

/** The run is drawn (or restored) when the screen starts collecting, not when the ViewModel is created. */
val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()
    .onStart { if (_uiState.value == QuizUiState.Loading) load() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizUiState.Loading)

private suspend fun load() {
    val content = contentRepository.content.first()
    val restored = restore(content)
    if (restored != null) show(restored) else start(content)
}
```

- **Guard on the state, not a flag:** `onStart` runs again each time sharing restarts (the screen comes back after
  5 s). Loading only while the state is still the loading state means a run is never drawn twice, and a load
  cancelled midway (screen left during loading) simply starts again.
- `load()` is a private `suspend fun`; it doesn't `launch`.
- Actions read and update `_uiState` (the source of truth), not `uiState`.

## Never

- `init { }` in a ViewModel, including `init { viewModelScope.launch { … } }`.
- A property initializer that starts work: `private val job = viewModelScope.launch { … }`.
- `SharingStarted.Eagerly` to force loading at creation (same problem as `init`). Use `WhileSubscribed(5_000)`.
- Collecting a flow inside the ViewModel to copy it into a `MutableStateFlow` (`progress.collect { _ui.value = … }`):
  use Pattern A.
- Exposing a `MutableStateFlow`; expose `StateFlow` only.

## Tests

The state only loads while collected, as on screen. In a ViewModel test, either:

- collect it with Turbine: `vm.uiState.test { val s = awaitItemMatching { !it.loading } … }`, or
- keep reading `.value` after `collecting(vm.uiState)` (`core:testing`), which collects for the rest of the test.

A test that reads `uiState.value` without collecting sees only the loading state: that's the rule working, not a
bug. With Pattern B, add a test that the setup happens once: collect, stop, `advanceTimeBy(10_000)`, collect
again, same state (see `QuizViewModelTest`, "the run is drawn once…").

## Checklist

```
ViewModel state:
- [ ] No init block, no work started in a property initializer
- [ ] uiState is derived (combine/map + stateIn) or loaded in onStart with a state guard (+ stateIn)
- [ ] SharingStarted.WhileSubscribed(5_000), initial value = the loading state
- [ ] In-screen choices in their own MutableStateFlow (+ SavedStateHandle if they must survive process death)
- [ ] Tests collect the state (Turbine or collecting()); Pattern B has a "loads once" test
- [ ] scripts/check-conventions.sh prints "Conventions OK."
```
