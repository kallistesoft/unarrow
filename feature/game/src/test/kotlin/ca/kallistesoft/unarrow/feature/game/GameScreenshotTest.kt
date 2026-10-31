package ca.kallistesoft.unarrow.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import ca.kallistesoft.unarrow.core.designsystem.PreviewSurface
import ca.kallistesoft.unarrow.core.game.Arrow
import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Direction
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice.DARK_FR
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice.LIGHT_EN
import ca.kallistesoft.unarrow.core.testing.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the game screen, light in English and dark in French (see README, "Screenshot tests"): a plain level,
 * a board with locks, keys and rotating arrows, the win panel and the out-of-lives panel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [ScreenshotDevice.SDK])
class GameScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    // A 4 × 4 corner with every kind of arrow: rotating, a blue key and its lock, an amber key and its lock, a violet one.
    private val mechanics = Board(
        4, 4,
        listOf(
            Arrow(0, 0, 0, Direction.UP, rotating = true),
            Arrow(1, 0, 2, Direction.RIGHT, key = 1),
            Arrow(2, 1, 1, Direction.LEFT, lock = 1),
            Arrow(3, 1, 3, Direction.DOWN, key = 0),
            Arrow(4, 2, 0, Direction.RIGHT, lock = 0),
            Arrow(5, 2, 2, Direction.UP),
            Arrow(6, 3, 1, Direction.LEFT, key = 2),
            Arrow(7, 3, 3, Direction.UP, lock = 2),
        ),
    )

    private fun session(status: GameStatus = GameStatus.PLAYING, lives: Int = MAX_LIVES) = GameSession(
        GameMode.LEVELS, 23, 0, mechanics, mechanics.arrows.size, lives = lives, taps = 14, seconds = 83,
        status = status, stars = if (status == GameStatus.WON) 2 else 0, tip = Tip.ROTATING,
    )

    private fun screen(state: GameUiState): @Composable () -> Unit = {
        PreviewSurface { GameContent(state, GameActions()) }
    }

    @Test
    @Config(qualifiers = LIGHT_EN)
    fun gameLightEn() = compose.captureScreenshot("game_light_en") { GamePreview() }

    @Test
    @Config(qualifiers = DARK_FR)
    fun gameDarkFr() = compose.captureScreenshot("game_dark_fr") { GamePreview() }

    @Test
    @Config(qualifiers = LIGHT_EN)
    fun mechanicsLightEn() = compose.captureScreenshot("game_mechanics_light_en", screen(GameUiState(session(), hints = 2)))

    @Test
    @Config(qualifiers = DARK_FR)
    fun mechanicsDarkFr() = compose.captureScreenshot("game_mechanics_dark_fr", screen(GameUiState(session(), hints = 2)))

    @Test
    @Config(qualifiers = LIGHT_EN)
    fun wonLightEn() = compose.captureScreenshot("game_won_light_en", screen(GameUiState(session(GameStatus.WON), bestStars = 3)))

    @Test
    @Config(qualifiers = DARK_FR)
    fun wonDarkFr() = compose.captureScreenshot("game_won_dark_fr", screen(GameUiState(session(GameStatus.WON), bestStars = 3)))

    @Test
    @Config(qualifiers = LIGHT_EN)
    fun lostLightEn() =
        compose.captureScreenshot("game_lost_light_en", screen(GameUiState(session(GameStatus.LOST, 0), rewardedReady = true)))

    @Test
    @Config(qualifiers = DARK_FR)
    fun lostDarkFr() =
        compose.captureScreenshot("game_lost_dark_fr", screen(GameUiState(session(GameStatus.LOST, 0), rewardedReady = true)))
}
