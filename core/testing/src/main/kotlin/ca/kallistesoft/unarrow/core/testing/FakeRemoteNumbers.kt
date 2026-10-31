package ca.kallistesoft.unarrow.core.testing

import ca.kallistesoft.unarrow.core.flags.RemoteNumber
import ca.kallistesoft.unarrow.core.flags.RemoteNumbers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Numbers at their defaults; [set] changes one as a Remote Config update would. */
class FakeRemoteNumbers : RemoteNumbers {
    override val numbers = MutableStateFlow(RemoteNumber.entries.associateWith { it.defaultValue })

    fun set(number: RemoteNumber, value: Long) = numbers.update { it + (number to value) }
}
