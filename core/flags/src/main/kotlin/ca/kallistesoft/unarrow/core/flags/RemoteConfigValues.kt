package ca.kallistesoft.unarrow.core.flags

/** Remote Config parameters other than feature flags (the force-update minimum). */
fun interface RemoteConfigValues {
    /**
     * Fetches the latest values, then reads [key] as a number: its value when the server has one (the last one fetched
     * if the server can't be reached now), 0 when the server has no such parameter, and null when nothing was ever
     * fetched or Firebase isn't configured.
     */
    suspend fun fetchLong(key: String): Long?
}

/** No Firebase (no google-services.json), and tests: nothing is known remotely. */
object NoRemoteConfigValues : RemoteConfigValues {
    override suspend fun fetchLong(key: String): Long? = null
}
