package ca.kallistesoft.unarrow.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.kallistesoft.unarrow.core.ads.BannerAds
import ca.kallistesoft.unarrow.core.ads.InterstitialAds
import ca.kallistesoft.unarrow.core.ads.RewardedAds
import ca.kallistesoft.unarrow.core.billing.Product
import ca.kallistesoft.unarrow.core.billing.Purchases
import ca.kallistesoft.unarrow.core.data.DailyResult
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.data.Today
import ca.kallistesoft.unarrow.core.engagement.ReviewPrompt
import ca.kallistesoft.unarrow.core.engagement.Sharer
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.game.Stars
import ca.kallistesoft.unarrow.core.game.TapResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One game screen: a puzzle, its lives and hints, then the next one. Tapping a blocked arrow costs a life; with none
 * left the puzzle is lost (a rewarded ad buys one more). A clock runs while the puzzle is played on screen, and a
 * solved puzzle gets its stars ([Stars]) and is saved at once; the interstitial (if one is due) comes when the player
 * moves on, never during a puzzle. The banner ([bannerAds]) sits under the board while
 * ads are allowed.
 */
@HiltViewModel(assistedFactory = GameViewModel.Factory::class)
class GameViewModel @AssistedInject constructor(
    @Assisted private val mode: GameMode,
    private val progress: ProgressRepository,
    private val today: Today,
    private val interstitials: InterstitialAds,
    private val rewarded: RewardedAds,
    private val purchases: Purchases,
    private val review: ReviewPrompt,
    private val sharer: Sharer,
    internal val bannerAds: BannerAds,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(mode: GameMode): GameViewModel
    }

    private val session = MutableStateFlow<GameSession?>(null)
    private var blockedSerial = 0
    private var clock: Job? = null

    /** A rewarded ad is on screen: the clock stops (the screen may not pause under it). */
    private var adOnScreen = false

    /** The puzzle is set up when the screen starts collecting, not when the ViewModel is created. */
    val uiState: StateFlow<GameUiState> = combine(
        session.onStart { if (session.value == null) load() },
        progress.progress,
        rewarded.rewardedReady,
        purchases.prices,
        bannerAds.bannerAllowed,
    ) { session, progress, rewardedReady, prices, banner ->
        val levels = session?.mode == GameMode.LEVELS
        GameUiState(
            session = session,
            hints = progress.hints,
            rewardedReady = rewardedReady,
            hintPackPrice = prices[Product.HINTS_10],
            soundOn = progress.soundOn,
            hapticsOn = progress.hapticsOn,
            banner = banner,
            bestStars = if (levels) progress.levelStars[session.level] else null,
            bestDaily = if (session?.mode == GameMode.DAILY) progress.bestDaily else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState())

    private suspend fun load() {
        val level = progress.progress.first().level
        session.value = when (mode) {
            GameMode.LEVELS -> levelSession(level)
            GameMode.DAILY -> {
                val day = today()
                val board = Levels.daily(day).board()
                GameSession(mode, level, Levels.dailyNumber(day), board, board.arrows.size)
            }
        }
    }

    private fun levelSession(level: Int): GameSession {
        val board = Levels.spec(level).board()
        return GameSession(GameMode.LEVELS, level, dailyNumber = 0, board, board.arrows.size)
    }

    fun tap(arrowId: Int) {
        val current = session.value ?: return
        if (current.status != GameStatus.PLAYING) return
        when (val result = current.board.tap(arrowId)) {
            is TapResult.Exited -> {
                val next = current.copy(
                    board = result.board,
                    taps = current.taps + 1,
                    hintId = current.hintId.takeIf { it != arrowId },
                    blocked = null,
                    exits = (current.exits + result.arrow).takeLast(EXITS_KEPT),
                )
                if (result.board.isCleared) {
                    val stars = Stars.of(current.startArrowCount, current.mistakes, current.seconds, current.hintUsed)
                    val won = next.copy(status = GameStatus.WON, stars = stars)
                    session.value = won
                    win(won)
                } else {
                    session.value = next
                }
            }
            is TapResult.Blocked -> {
                val lives = current.lives - 1
                session.value = current.copy(
                    lives = lives,
                    taps = current.taps + 1,
                    mistakes = current.mistakes + 1,
                    blocked = BlockedTap(arrowId, result.blocker.id, ++blockedSerial),
                    status = if (lives <= 0) GameStatus.LOST else GameStatus.PLAYING,
                )
            }
            TapResult.Missed -> Unit
        }
    }

    private fun win(played: GameSession) {
        viewModelScope.launch {
            when (played.mode) {
                GameMode.LEVELS -> progress.completeLevel(played.level, played.stars)
                GameMode.DAILY -> progress.completeDaily(DailyResult(played.stars, played.seconds))
            }
            review.afterWin()
        }
    }

    /** Points at an arrow that can leave now: a hint from the player's stock, or a rewarded ad when there's none. */
    fun hint() {
        val current = session.value ?: return
        if (current.status != GameStatus.PLAYING || current.hintId != null) return
        viewModelScope.launch {
            val paid = progress.useHint() || watchRewarded()
            if (!paid) return@launch
            session.update { s -> s?.copy(hintId = s.board.freeArrows().firstOrNull()?.id, hintUsed = true) }
        }
    }

    /** Lost: a rewarded ad watched to the end gives one more life on the same board. */
    fun extraLife() {
        if (session.value?.status != GameStatus.LOST) return
        viewModelScope.launch {
            if (!watchRewarded()) return@launch
            session.update { it?.copy(lives = 1, blocked = null, status = GameStatus.PLAYING) }
        }
    }

    /** The same puzzle from the start. */
    fun restart() {
        val current = session.value ?: return
        session.value = when (current.mode) {
            GameMode.LEVELS -> levelSession(current.level)
            GameMode.DAILY -> {
                val board = Levels.daily(today()).board()
                GameSession(GameMode.DAILY, current.level, current.dailyNumber, board, board.arrows.size)
            }
        }
    }

    /** The screen is on (resumed): the clock runs, a second at a time, while the puzzle is played. */
    fun startClock() {
        if (clock?.isActive == true) return
        clock = viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (!adOnScreen) {
                    session.update { s -> if (s?.status == GameStatus.PLAYING) s.copy(seconds = s.seconds + 1) else s }
                }
            }
        }
    }

    /** The screen is paused (app in the background, another screen on top): the clock stops. */
    fun stopClock() {
        clock?.cancel()
    }

    private suspend fun watchRewarded(): Boolean {
        adOnScreen = true
        try {
            return rewarded.showRewarded()
        } finally {
            adOnScreen = false
        }
    }

    /** After a solved level: the interstitial if one is due, then the next level. */
    fun next() {
        val current = session.value ?: return
        if (current.status != GameStatus.WON || current.mode != GameMode.LEVELS) return
        viewModelScope.launch {
            interstitials.showAfterPuzzle(current.level)
            session.value = levelSession(current.level + 1)
        }
    }

    /** Leaving after a solved daily: its interstitial (if due) shows before going home. */
    suspend fun leaveAfterWin() {
        val current = session.value ?: return
        if (current.status == GameStatus.WON) interstitials.showAfterPuzzle(current.level)
    }

    fun buyHints() {
        viewModelScope.launch { purchases.buy(Product.HINTS_10) }
    }

    fun share(text: String, chooserTitle: String) = sharer.share(text, chooserTitle)
}
