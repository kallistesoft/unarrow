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
    /** Lock and key colours (amber, blue, violet), each also drawn with its own shape for colour-blind players. */
    val locks: List<Color>,
)

// Kallistesoft "Nocturne" (kallistesoft.ca, assets/style.css): the studio's palette, in both themes.
private object Nocturne {
    val accent = Color(0xFF2F6BFF)
    val coral = Color(0xFFE8604C)
    val amber = Color(0xFFF2B134)
    val white = Color(0xFFFFFFFF)

    object Light {
        val bg = Color(0xFFF6F8FC)
        val bg2 = Color(0xFFEDF1F9)
        val surface = Color(0xFFFFFFFF)
        val surfaceHover = Color(0xFFF3F6FF)
        val line = Color(0xFFDFE5F1)
        val card = Color(0xFFDBE2F0)
        val line2 = Color(0xFFC3CDE3)
        val text = Color(0xFF0A1330)
        val muted = Color(0xFF3F4B6B)
        val muted2 = Color(0xFF56627F)
        val accentText = Color(0xFF1F4FD6)
        val chip = Color(0xFFEAF0FF)
        val chipText = Color(0xFF1F3A8A)
        val ok = Color(0xFF1A9C66)
        val cell = Color(0xFFE4E9F4)
    }

    object Dark {
        val bg = Color(0xFF060D24)
        val bg2 = Color(0xFF08112C)
        val surface = Color(0xFF0B1636)
        val surfaceHover = Color(0xFF101C44)
        val line = Color(0xFF16224A)
        val card = Color(0xFF1C2A58)
        val line2 = Color(0xFF2A3A70)
        val text = Color(0xFFE9EFFF)
        val muted = Color(0xFFB8C6EA)
        val muted2 = Color(0xFF8FA2CF)
        val accentText = Color(0xFF8FB0FF)
        val chip = Color(0xFF13204A)
        val chipText = Color(0xFFC9D6FF)
        val ok = Color(0xFF3DDC97)
        val cell = Color(0xFF13204A)
    }
}

/** The studio's colours that Material 3 has no role for: card borders, chips, link text, the "ok" dot. */
@Immutable
data class BrandColors(
    /** 1 px border of cards and panels. */
    val cardBorder: Color,
    /** A card or row while pressed. */
    val surfaceHover: Color,
    val chip: Color,
    val onChip: Color,
    /** Accent used for text (section titles, links): readable on the background in both themes. */
    val accentText: Color,
    /** Secondary text, one step quieter than onSurfaceVariant. */
    val muted2: Color,
    val ok: Color,
)

private val LightBrand = with(Nocturne.Light) {
    BrandColors(card, surfaceHover, chip, chipText, accentText, muted2, ok)
}
private val DarkBrand = with(Nocturne.Dark) {
    BrandColors(card, surfaceHover, chip, chipText, accentText, muted2, ok)
}

// Lock and key colours from the studio palette (amber, blue, violet), each also drawn with its own shape. Light
// variants are darkened to stay at 3:1 or more on the light tiles; dark ones lightened on the dark tiles.
private val LightGame = GameColors(
    arrow = Nocturne.Light.text,
    tile = Nocturne.Light.cell,
    hint = Color(0xFF148A59),
    blocked = Nocturne.coral,
    life = Nocturne.coral,
    streak = Nocturne.amber,
    star = Nocturne.amber,
    locks = listOf(Color(0xFFA8700F), Color(0xFF2272B0), Color(0xFF8A5CD1)),
)
private val DarkGame = GameColors(
    arrow = Nocturne.Dark.text,
    tile = Nocturne.Dark.cell,
    hint = Nocturne.Dark.ok,
    blocked = Nocturne.coral,
    life = Nocturne.coral,
    streak = Nocturne.amber,
    star = Nocturne.amber,
    locks = listOf(Nocturne.amber, Color(0xFF5AA6E0), Color(0xFFA98BE3)),
)

private val LightColors = with(Nocturne.Light) {
    lightColorScheme(
        primary = Nocturne.accent,
        onPrimary = Nocturne.white,
        primaryContainer = chip,
        onPrimaryContainer = chipText,
        secondary = muted,
        onSecondary = Nocturne.white,
        secondaryContainer = chip,
        onSecondaryContainer = chipText,
        tertiary = ok,
        onTertiary = Nocturne.white,
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = bg2,
        onSurfaceVariant = muted,
        surfaceBright = surface,
        surfaceDim = bg2,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceHover,
        surfaceContainerHighest = bg2,
        outline = line2,
        outlineVariant = line,
        error = Color(0xFFC94A37),
        onError = Nocturne.white,
    )
}
private val DarkColors = with(Nocturne.Dark) {
    darkColorScheme(
        primary = Nocturne.accent,
        onPrimary = Nocturne.white,
        primaryContainer = chip,
        onPrimaryContainer = chipText,
        secondary = muted,
        onSecondary = bg,
        secondaryContainer = chip,
        onSecondaryContainer = chipText,
        tertiary = ok,
        onTertiary = bg,
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = bg2,
        onSurfaceVariant = muted,
        surfaceBright = surfaceHover,
        surfaceDim = bg,
        surfaceContainerLowest = bg2,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceHover,
        surfaceContainerHighest = bg2,
        outline = line2,
        outlineVariant = line,
        error = Color(0xFFFF8A78),
        onError = bg,
    )
}

val LocalGameColors = staticCompositionLocalOf { LightGame }
val LocalBrandColors = staticCompositionLocalOf { LightBrand }

/** The site's corners: small buttons 10, buttons 12, cards 18. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Sizes.cornerXs),
    small = RoundedCornerShape(Sizes.cornerS),
    medium = RoundedCornerShape(Sizes.cornerM),
    large = RoundedCornerShape(Sizes.cornerL),
    extraLarge = RoundedCornerShape(Sizes.cornerXl),
)

/**
 * Unarrow's theme, the studio's "Nocturne" look: fixed colours (no dynamic colour, so the brand always shows),
 * Manrope type and the site's corners. [darkTheme] follows the system unless the player picked a theme in Settings.
 */
@Composable
fun UnarrowTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGameColors provides if (darkTheme) DarkGame else LightGame,
        LocalBrandColors provides if (darkTheme) DarkBrand else LightBrand,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object UnarrowTheme {
    val gameColors: GameColors
        @Composable get() = LocalGameColors.current

    val brand: BrandColors
        @Composable get() = LocalBrandColors.current
}
