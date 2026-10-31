package ca.kallistesoft.unarrow.core.flags

/**
 * Features that can be switched on or off remotely, without a release. [key] is the Firebase Remote Config parameter
 * (Boolean); [defaultValue] applies until the console sets a value, and when Firebase isn't configured or reachable.
 */
enum class FeatureFlag(val key: String, val defaultValue: Boolean) {
    /** Every ad (AdMob). Off: no ad is loaded or shown. */
    ADS_ENABLED("ads_enabled", true),

    /** "Watch an ad" for a hint or an extra life. */
    REWARDED_ADS("rewarded_ads", true),

    /** Asking for a Play rating after a few solved puzzles. */
    REVIEW_PROMPT("review_prompt", true),
}
