package ca.kallistesoft.unarrow.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.kallistesoft.unarrow.core.billing.Product
import ca.kallistesoft.unarrow.core.billing.Purchases
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.data.Today
import ca.kallistesoft.unarrow.core.game.Levels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    progress: ProgressRepository,
    private val purchases: Purchases,
    today: Today,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(progress.progress, purchases.prices) { p, prices ->
        val day = today()
        HomeUiState(
            loading = false,
            level = p.level,
            dailyNumber = Levels.dailyNumber(day),
            dailyDone = p.dailyDone(day),
            streak = p.currentStreak(day),
            removeAdsOwned = p.removeAdsOwned,
            removeAdsPrice = prices[Product.REMOVE_ADS],
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun buyRemoveAds() {
        viewModelScope.launch { purchases.buy(Product.REMOVE_ADS) }
    }
}
