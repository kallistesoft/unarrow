package ca.kallistesoft.unarrow.navigation

import androidx.navigation3.runtime.NavKey
import ca.kallistesoft.unarrow.feature.game.GameMode
import kotlinx.serialization.Serializable

// Destinations. Serializable so the back stack survives configuration changes and process death.

@Serializable
data object Home : NavKey

/**
 * A game: the player's levels, or today's daily puzzle; or one puzzle played again after a rewarded ad ([replay]: the
 * level, or the daily's epoch day), from the history or from home.
 */
@Serializable
data class Game(val mode: GameMode, val replay: Long? = null, val fromHistory: Boolean = false) : NavKey

/** Every solved level and daily, with their best results. */
@Serializable
data object History : NavKey

@Serializable
data object Settings : NavKey
