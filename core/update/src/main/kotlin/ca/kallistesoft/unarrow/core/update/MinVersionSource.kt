package ca.kallistesoft.unarrow.core.update

import ca.kallistesoft.unarrow.core.flags.RemoteConfigValues
import javax.inject.Inject

/** Where the minimum allowed versionCode comes from. */
fun interface MinVersionSource {
    /** The current minimum (0 for none), or null when it can't be known (offline before any fetch, no Firebase). */
    suspend fun fetch(parameter: String): Long?
}

/** The minimum is a Firebase Remote Config parameter (see README, "Force update"). */
internal class RemoteConfigMinVersionSource @Inject constructor(
    private val remoteConfig: RemoteConfigValues,
) : MinVersionSource {
    override suspend fun fetch(parameter: String): Long? = remoteConfig.fetchLong(parameter)
}
