package ca.kallistesoft.unarrow.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.kallistesoft.unarrow.core.ads.RewardedAds
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.game.Levels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryTab { LEVELS, DAILY }

/** A solved level and its best result; [seconds] is null when it was solved before times were kept. */
data class LevelEntry(val level: Int, val stars: Int, val seconds: Int?)

/** A solved daily ([day]: epoch day, [number]: "Daily #12") and its best result. */
data class DailyEntry(val day: Long, val number: Long, val stars: Int, val seconds: Int)

data class HistoryUiState(
    val loading: Boolean = true,
    val tab: HistoryTab = HistoryTab.LEVELS,
    /** Highest level first. */
    val levels: List<LevelEntry> = emptyList(),
    /** Newest first. */
    val dailies: List<DailyEntry> = emptyList(),
    /** A rewarded ad is loaded: replays are offered only while true. */
    val rewardedReady: Boolean = false,
)

/** Every solved level and daily with its best result; any of them can be played again after a rewarded ad. */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    progress: ProgressRepository,
    private val rewarded: RewardedAds,
) : ViewModel() {
    private val tab = MutableStateFlow(HistoryTab.LEVELS)

    val uiState: StateFlow<HistoryUiState> =
        combine(progress.progress, rewarded.rewardedReady, tab) { p, ready, tab ->
            HistoryUiState(
                loading = false,
                tab = tab,
                levels = p.levelResults.map { (level, result) -> LevelEntry(level, result.stars, result.seconds) }
                    .sortedByDescending { it.level },
                dailies = p.dailyResults.map { (day, result) ->
                    DailyEntry(day, Levels.dailyNumber(day), result.stars, result.seconds)
                }.sortedByDescending { it.day },
                rewardedReady = ready,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectTab(selected: HistoryTab) {
        tab.value = selected
    }

    /** Shows a rewarded ad; [onWatched] opens the replay only when it was watched to the end. */
    fun replay(onWatched: () -> Unit) {
        viewModelScope.launch { if (rewarded.showRewarded()) onWatched() }
    }
}
