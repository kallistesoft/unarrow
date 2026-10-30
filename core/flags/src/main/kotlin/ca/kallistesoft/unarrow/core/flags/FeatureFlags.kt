package ca.kallistesoft.unarrow.core.flags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface FeatureFlags {
    /** Every flag's current value; emits again when new remote values are activated. */
    val values: Flow<Map<FeatureFlag, Boolean>>

    /** Fetches and activates the latest remote values (throttled by the fetch interval). */
    suspend fun refresh()

    fun isEnabled(flag: FeatureFlag): Flow<Boolean> = values.map { it.getValue(flag) }.distinctUntilChanged()
}

/** How often Remote Config may fetch; the app sets 0 for debug builds so console changes show at once. */
data class FeatureFlagSettings(val minimumFetchIntervalSeconds: Long)

/** Defaults only: used when Firebase isn't configured (no google-services.json), and in tests. */
class DefaultFeatureFlags(
    private val overrides: Map<FeatureFlag, Boolean> = emptyMap(),
) : FeatureFlags {
    override val values: Flow<Map<FeatureFlag, Boolean>> =
        flowOf(FeatureFlag.entries.associateWith { overrides[it] ?: it.defaultValue })

    override suspend fun refresh() = Unit
}
