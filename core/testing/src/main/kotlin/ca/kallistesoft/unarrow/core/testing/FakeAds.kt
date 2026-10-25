package ca.kallistesoft.unarrow.core.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ca.kallistesoft.unarrow.core.ads.BannerAds
import ca.kallistesoft.unarrow.core.ads.InterstitialAds
import ca.kallistesoft.unarrow.core.ads.RewardedAds
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Records the interstitials asked for; the rewarded ad is [rewardedReady] and pays out when [earn]. The banner is
 * allowed while [bannerAllowed], and is an empty box tagged "banner".
 */
class FakeAds(ready: Boolean = true, var earn: Boolean = true, banner: Boolean = true) :
    InterstitialAds, RewardedAds, BannerAds {
    val interstitialLevels = mutableListOf<Int>()
    var rewardedShown = 0
    override val rewardedReady = MutableStateFlow(ready)
    override val bannerAllowed = MutableStateFlow(banner)

    override suspend fun showAfterPuzzle(level: Int) {
        interstitialLevels += level
    }

    override suspend fun showRewarded(): Boolean {
        rewardedShown++
        return rewardedReady.value && earn
    }

    @Composable
    override fun Banner(modifier: Modifier) = Box(modifier.testTag("banner"))
}
