package ca.kallistesoft.unarrow.core.flags

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.google.firebase.remoteconfig.configUpdates
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Feature flags, numbers and other values from Firebase Remote Config. Values fetched on a previous launch apply at start; new
 * ones arrive on [refresh] (app back in the foreground) and through Remote Config's real-time updates while the app
 * is open.
 */
internal class FirebaseFeatureFlags(
    private val remoteConfig: FirebaseRemoteConfig,
    settings: FeatureFlagSettings,
    scope: CoroutineScope,
) : FeatureFlags, RemoteNumbers, RemoteConfigValues {
    private val state = MutableStateFlow(read())
    override val values: StateFlow<Map<FeatureFlag, Boolean>> = state.asStateFlow()

    private val numberState = MutableStateFlow(readNumbers())
    override val numbers: StateFlow<Map<RemoteNumber, Long>> = numberState.asStateFlow()

    init {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings { minimumFetchIntervalInSeconds = settings.minimumFetchIntervalSeconds },
        )
        scope.launch {
            remoteConfig.configUpdates
                .catch { /* Real-time updates unavailable: fetches on refresh() still work. */ }
                .collect {
                    runCatching { remoteConfig.activate().await() }
                    update()
                }
        }
    }

    override suspend fun refresh() {
        runCatching { remoteConfig.fetchAndActivate().await() } // Offline: keep the last activated values.
        update()
    }

    override suspend fun fetchLong(key: String): Long? {
        val fetched = runCatching { remoteConfig.fetchAndActivate().await() }.isSuccess
        update()
        val value = remoteConfig.getValue(key)
        return when {
            value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> value.asLong()
            fetched -> 0 // The server answered without this parameter.
            else -> null
        }
    }

    private fun update() {
        state.value = read()
        numberState.value = readNumbers()
    }

    /** A value only counts once it comes from the server; otherwise the flag's own default applies. */
    private fun read(): Map<FeatureFlag, Boolean> = FeatureFlag.entries.associateWith { flag ->
        val value = remoteConfig.getValue(flag.key)
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) value.asBoolean() else flag.defaultValue
    }

    /** As for flags, and a value that isn't a whole number or is below the minimum gets the default too. */
    private fun readNumbers(): Map<RemoteNumber, Long> = RemoteNumber.entries.associateWith { number ->
        val value = remoteConfig.getValue(number.key)
        val remote = if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) value.asLongOrNull() else null
        remote?.takeIf { it >= number.minimum } ?: number.defaultValue
    }

    private fun FirebaseRemoteConfigValue.asLongOrNull(): Long? = runCatching { asLong() }.getOrNull()

    companion object {
        fun create(settings: FeatureFlagSettings, scope: CoroutineScope): FirebaseFeatureFlags =
            FirebaseFeatureFlags(Firebase.remoteConfig, settings, scope)
    }
}
