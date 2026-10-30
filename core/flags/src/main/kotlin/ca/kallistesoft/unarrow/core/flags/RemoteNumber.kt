package ca.kallistesoft.unarrow.core.flags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Numbers that can be tuned remotely, without a release. [key] is the Firebase Remote Config parameter (Number);
 * [defaultValue] applies until the console sets a value, when Firebase isn't configured or reachable, and when the
 * console's value is below [minimum] or not a whole number.
 */
enum class RemoteNumber(val key: String, val defaultValue: Long, val minimum: Long) {
    /** The first level after which interstitials may show: new players learn the game without ads. */
    INTERSTITIAL_FIRST_LEVEL("interstitial_first_level", 5, 1),

    /** One interstitial every this many solved puzzles. */
    INTERSTITIAL_EVERY("interstitial_every", 3, 1),

    /** At most one interstitial every this many seconds. */
    INTERSTITIAL_CAP_SECONDS("interstitial_cap_seconds", 180, 0),
}

interface RemoteNumbers {
    /** Every number's current value; emits again when new remote values are activated. */
    val numbers: Flow<Map<RemoteNumber, Long>>

    fun value(number: RemoteNumber): Flow<Long> = numbers.map { it.getValue(number) }.distinctUntilChanged()
}

/** Defaults only: used when Firebase isn't configured (no google-services.json), and in tests. */
class DefaultRemoteNumbers(
    private val overrides: Map<RemoteNumber, Long> = emptyMap(),
) : RemoteNumbers {
    override val numbers: Flow<Map<RemoteNumber, Long>> =
        flowOf(RemoteNumber.entries.associateWith { overrides[it] ?: it.defaultValue })
}
