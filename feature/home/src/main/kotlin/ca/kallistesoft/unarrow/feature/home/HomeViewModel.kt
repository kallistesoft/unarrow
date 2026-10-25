package ca.kallistesoft.unarrow.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.kallistesoft.unarrow.core.ads.RewardedAds
import ca.kallistesoft.unarrow.core.billing.Product
import ca.kallistesoft.unarrow.core.billing.Purchases
import ca.kallistesoft.unarrow.core.data.DailyResult
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.data.Today
import ca.kallistesoft.unarrow.core.game.Levels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val level: Int = 1,
    val dailyNumber: Long = 1,
    val dailyDone: Boolean = false,
    val streak: Int = 0,
    val removeAdsOwned: Boolean = false,
    /** Price of "Remove ads", null while Play hasn't given one (not for sale now). */
    val removeAdsPrice: String? = null,
    /** A rewarded ad is ready: a solved daily can be played again after watching it. */
    val rewardedReady: Boolean = false,
    /** Today's result, once today's daily is solved. */
    val dailyResult: DailyResult? = null,
    /** Records: every star earned (the best of each level) and the longest daily streak. */
    val totalStars: Int = 0,
    val bestStreak: Int = 0,
    val soundOn: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progress: ProgressRepository,
    private val purchases: Purchases,
    private val rewarded: RewardedAds,
    today: Today,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        progress.progress,
        purchases.prices,
        rewarded.rewardedReady,
    ) { p, prices, rewardedReady ->
        val day = today()
        HomeUiState(
            loading = false,
            level = p.level,
            dailyNumber = Levels.dailyNumber(day),
            dailyDone = p.dailyDone(day),
            streak = p.currentStreak(day),
            removeAdsOwned = p.removeAdsOwned,
            removeAdsPrice = prices[Product.REMOVE_ADS],
            rewardedReady = rewardedReady,
            dailyResult = p.dailyResult(day),
            totalStars = p.totalStars,
            bestStreak = p.bestDailyStreak,
            soundOn = p.soundOn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /**
     * Today's daily again, once a rewarded ad was watched to the end. The streak doesn't move (one day counts once);
     * only a better result can replace the best daily.
     */
    fun retryDaily(onRetry: () -> Unit) {
        viewModelScope.launch { if (rewarded.showRewarded()) onRetry() }
    }

    /** The home's sound button: the same setting as in Settings. */
    fun toggleSound() {
        viewModelScope.launch { progress.setSound(!progress.progress.first().soundOn) }
    }

    fun buyRemoveAds() {
        viewModelScope.launch { purchases.buy(Product.REMOVE_ADS) }
    }
}
