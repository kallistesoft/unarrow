package ca.kallistesoft.unarrow.core.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The interstitial between puzzles: after every [INTERSTITIAL_EVERY] solved puzzles from level [FIRST_AD_LEVEL], never
 * during a puzzle, and never once "Remove ads" is bought. Tests use a fake (`FakeAds` in `:core:testing`).
 */
fun interface InterstitialAds {
    /**
     * Called once a puzzle is solved, before the next one; returns once the ad (if any) is closed. [level] is the
     * player's level (the daily puzzle passes theirs too). Returns at once, showing nothing, when no ad is due or
     * none can be shown (no consent, `ads_enabled` off, the 3-minute cap, none loaded).
     */
    suspend fun showAfterPuzzle(level: Int)
}

/** The ad a player chooses to watch for a reward: a hint, or one more life after running out. */
interface RewardedAds {
    /** A rewarded ad is loaded and may be shown: the screen offers "Watch an ad" only while true. */
    val rewardedReady: StateFlow<Boolean>

    /** Shows the rewarded ad; true when the player watched it to the end and earned the reward. */
    suspend fun showRewarded(): Boolean
}

/**
 * The banner strip under the board while playing. Shown only while ads may be requested (as for interstitials:
 * consent, `ads_enabled`), and never once "Remove ads" is bought.
 */
interface BannerAds {
    /** The screen places [Banner] only while true. */
    val bannerAllowed: Flow<Boolean>

    /** An anchored banner, as wide as [modifier] allows; it takes no room until an ad has loaded. */
    @Composable
    fun Banner(modifier: Modifier)
}

/** The consent to ads (Google's User Messaging Platform), asked at launch and changed from Settings. */
interface AdConsent {
    /** The user's region requires a way back to their consent choices: Settings shows "Privacy options" while true. */
    val privacyOptionsRequired: StateFlow<Boolean>

    /**
     * At launch: refreshes the consent information and shows the consent form if it's required. Ads start (Mobile
     * Ads initialised, the first ads loading) only once ads may be requested.
     */
    fun gatherConsent(activity: Activity)

    /** Settings → Privacy options: shows the consent choices again, over the app's current screen. */
    fun showPrivacyOptions()
}

/**
 * The ad units; the build type chooses them (Google's test units outside release, see app/build.gradle.kts).
 * [enabled] false turns ads off entirely, consent request included (instrumented tests).
 */
data class AdsConfig(
    val interstitialUnitId: String,
    val rewardedUnitId: String = "",
    val bannerUnitId: String = "",
    val enabled: Boolean = true,
)

/** The first level after which interstitials may show: new players learn the game without ads. */
const val FIRST_AD_LEVEL = 5

/** One interstitial every this many solved puzzles. */
const val INTERSTITIAL_EVERY = 3
