package ca.kallistesoft.unarrow.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The board's colours, which Material 3 has no role for. */
@Immutable
data class GameColors(
    /** An arrow at rest. */
    val arrow: Color,
    /** The tile under each cell of the grid. */
    val tile: Color,
    /** The hinted arrow, and the "solved" check. */
    val hint: Color,
    /** An arrow that was tapped but can't leave, and the arrow in its way. */
    val blocked: Color,
    /** Lives (hearts). */
    val life: Color,
    /** The daily streak flame. */
    val streak: Color,
    /** Stars earned on a solved puzzle. */
    val star: Color,
)

private val LightGame = GameColors(
    arrow = Color(0xFF1B1D24),
    tile = Color(0xFFE9E6DF),
    hint = Color(0xFF00A86B),
    blocked = Color(0xFFE5484D),
    life = Color(0xFFE5484D),
    streak = Color(0xFFF2711C),
    star = Color(0xFFE8A200),
)
private val DarkGame = GameColors(
    arrow = Color(0xFFECEDF2),
    tile = Color(0xFF262A33),
    hint = Color(0xFF3DD68C),
    blocked = Color(0xFFFF6B6B),
    life = Color(0xFFFF6B6B),
    streak = Color(0xFFFF8A3D),
    star = Color(0xFFFFC233),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D5AFE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001257),
    secondary = Color(0xFF5B5D72),
    background = Color(0xFFF6F4EF),
    onBackground = Color(0xFF1B1D24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1D24),
    surfaceVariant = Color(0xFFEFECE6),
    onSurfaceVariant = Color(0xFF5A5C66),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1EEE8),
    outline = Color(0xFFD5D2CB),
    error = Color(0xFFE5484D),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8C9EFF),
    onPrimary = Color(0xFF00218B),
    primaryContainer = Color(0xFF2A3A9E),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC3C5DD),
    background = Color(0xFF12141A),
    onBackground = Color(0xFFECEDF2),
    surface = Color(0xFF1C1F27),
    onSurface = Color(0xFFECEDF2),
    surfaceVariant = Color(0xFF232731),
    onSurfaceVariant = Color(0xFFA6A9B6),
    surfaceContainer = Color(0xFF1C1F27),
    surfaceContainerHigh = Color(0xFF262A33),
    outline = Color(0xFF343844),
    error = Color(0xFFFF6B6B),
)

val LocalGameColors = staticCompositionLocalOf { LightGame }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Sizes.cornerXs),
    small = RoundedCornerShape(Sizes.cornerS),
    medium = RoundedCornerShape(Sizes.cornerM),
    large = RoundedCornerShape(Sizes.cornerL),
    extraLarge = RoundedCornerShape(Sizes.cornerXl),
)

/** Unarrow's theme: fixed colours (no dynamic colour), so the game always looks like its store listing. */
@Composable
fun UnarrowTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGameColors provides if (darkTheme) DarkGame else LightGame) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = AppShapes,
            content = content,
        )
    }
}

object UnarrowTheme {
    val gameColors: GameColors
        @Composable get() = LocalGameColors.current
}
