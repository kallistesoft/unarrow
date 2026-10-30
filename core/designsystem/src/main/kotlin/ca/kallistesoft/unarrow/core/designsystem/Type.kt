package ca.kallistesoft.unarrow.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** One face of a variable font at [weight]. */
private fun variable(resource: Int, weight: Int) = Font(
    resource,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Manrope (SIL OFL, bundled): every text in the app, as on kallistesoft.ca. */
val Manrope = FontFamily(
    listOf(400, 500, 600, 700, 800).map { variable(R.font.manrope, it) },
)

/** JetBrains Mono (SIL OFL, bundled): small technical labels, the timer and counters. */
val JetBrainsMono = FontFamily(
    listOf(500, 700).map { variable(R.font.jetbrains_mono, it) },
)

/** The studio's type ("Nocturne"): extra-bold headings with tight letter spacing, Manrope throughout. */
internal val AppTypography: Typography = Typography().let { base ->
    fun TextStyle.manrope(weight: Int, spacing: Double = 0.0) =
        copy(fontFamily = Manrope, fontWeight = FontWeight(weight), letterSpacing = spacing.em)
    Typography(
        displayLarge = base.displayLarge.manrope(800, DISPLAY_SPACING),
        displayMedium = base.displayMedium.manrope(800, DISPLAY_SPACING),
        displaySmall = base.displaySmall.manrope(800, DISPLAY_SPACING),
        headlineLarge = base.headlineLarge.manrope(800, HEADING_SPACING),
        headlineMedium = base.headlineMedium.manrope(800, HEADING_SPACING),
        headlineSmall = base.headlineSmall.manrope(800, HEADING_SPACING),
        titleLarge = base.titleLarge.manrope(800, TITLE_SPACING),
        titleMedium = base.titleMedium.manrope(700, TITLE_SPACING),
        titleSmall = base.titleSmall.manrope(700),
        bodyLarge = base.bodyLarge.manrope(500),
        bodyMedium = base.bodyMedium.manrope(500),
        bodySmall = base.bodySmall.manrope(500),
        labelLarge = base.labelLarge.manrope(700),
        labelMedium = base.labelMedium.manrope(600),
        labelSmall = base.labelSmall.manrope(600),
    )
}

/** Small technical labels in JetBrains Mono (the timer, counters, "DAILY #12"), like the site's section labels. */
val MonoLabel = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.02.em)

private const val DISPLAY_SPACING = -0.04
private const val HEADING_SPACING = -0.035
private const val TITLE_SPACING = -0.02
