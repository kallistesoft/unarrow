package ca.kallistesoft.unarrow.core.update

/**
 * Where the app learns which versions are still allowed, and who it is. Provided by the app module so each build type
 * can use its own Remote Config parameter.
 */
data class ForceUpdateSettings(
    /** Remote Config parameter holding the minimum versionCode, e.g. `min_version_code`; see the README. */
    val parameter: String,
    val currentVersionCode: Long,
    /** Play Store listing to open when in-app updates are unavailable (the release application ID). */
    val storePackage: String,
)

/** An installed build older than the minimum must update before it can be used. */
fun isUpdateRequired(currentVersionCode: Long, minVersionCode: Long?): Boolean =
    minVersionCode != null && currentVersionCode < minVersionCode
