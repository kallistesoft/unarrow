package ca.kallistesoft.unarrow.core.testing

import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Progress in memory, with the same rules as the DataStore repository; [today] is the current epoch day. */
class FakeProgressRepository(initial: GameProgress = GameProgress(), var today: Long = 20_800L) : ProgressRepository {
    val state = MutableStateFlow(initial)
    override val progress: StateFlow<GameProgress> = state

    override suspend fun completeLevel(level: Int) {
        state.value = state.value.let { it.copy(level = maxOf(it.level, level + 1), wins = it.wins + 1) }
    }

    override suspend fun completeDaily() {
        val p = state.value
        if (p.lastDailyDay == today) return
        val streak = if (p.lastDailyDay == today - 1) p.dailyStreak + 1 else 1
        state.value = p.copy(
            dailyStreak = streak,
            bestDailyStreak = maxOf(p.bestDailyStreak, streak),
            lastDailyDay = today,
            wins = p.wins + 1,
        )
    }

    override suspend fun useHint(): Boolean {
        val p = state.value
        if (p.hints <= 0) return false
        state.value = p.copy(hints = p.hints - 1)
        return true
    }

    override suspend fun addHints(count: Int) {
        state.value = state.value.let { it.copy(hints = it.hints + count) }
    }

    override suspend fun setSound(on: Boolean) {
        state.value = state.value.copy(soundOn = on)
    }

    override suspend fun setHaptics(on: Boolean) {
        state.value = state.value.copy(hapticsOn = on)
    }

    override suspend fun markReviewPrompted() {
        state.value = state.value.copy(lastReviewPromptDay = today)
    }

    override suspend fun setRemoveAdsOwned(owned: Boolean) {
        state.value = state.value.copy(removeAdsOwned = owned)
    }
}
