package ca.kallistesoft.unarrow.di

/**
 * Settings that differ between build types. Each build type provides its own from `src/debug` or `src/release`
 * (`BuildTypeModule`), so the code never branches on the build type.
 */
data class BuildTypeConfig(
    /** Remote Config parameter holding the minimum versionCode (force update). */
    val minVersionParameter: String,
    /** Minimum time between two Remote Config fetches. */
    val featureFlagFetchIntervalSeconds: Long,
)
