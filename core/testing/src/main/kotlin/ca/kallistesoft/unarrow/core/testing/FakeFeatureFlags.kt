package ca.kallistesoft.unarrow.core.testing

import ca.kallistesoft.unarrow.core.flags.FeatureFlag
import ca.kallistesoft.unarrow.core.flags.FeatureFlags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Flags at their defaults, except [off]; [set] changes one as a Remote Config update would. */
class FakeFeatureFlags(vararg off: FeatureFlag) : FeatureFlags {
    override val values = MutableStateFlow(FeatureFlag.entries.associateWith { it !in off && it.defaultValue })

    fun set(flag: FeatureFlag, enabled: Boolean) = values.update { it + (flag to enabled) }

    override suspend fun refresh() = Unit
}
