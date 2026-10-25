package ca.kallistesoft.unarrow.core.testing

import ca.kallistesoft.unarrow.core.ads.InterstitialAds
import ca.kallistesoft.unarrow.core.ads.RewardedAds
import kotlinx.coroutines.flow.MutableStateFlow

/** Records the interstitials asked for; the rewarded ad is [rewardedReady] and pays out when [earn]. */
class FakeAds(ready: Boolean = true, var earn: Boolean = true) : InterstitialAds, RewardedAds {
    val interstitialLevels = mutableListOf<Int>()
    var rewardedShown = 0
    override val rewardedReady = MutableStateFlow(ready)

    override suspend fun showAfterPuzzle(level: Int) {
        interstitialLevels += level
    }

    override suspend fun showRewarded(): Boolean {
        rewardedShown++
        return rewardedReady.value && earn
    }
}
