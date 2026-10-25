package ca.kallistesoft.unarrow.navigation

import androidx.navigation3.runtime.NavKey
import ca.kallistesoft.unarrow.feature.game.GameMode
import kotlinx.serialization.Serializable

// Destinations. Serializable so the back stack survives configuration changes and process death.

@Serializable
data object Home : NavKey

/** A game: the player's levels, or today's daily puzzle. */
@Serializable
data class Game(val mode: GameMode) : NavKey

@Serializable
data object Settings : NavKey
