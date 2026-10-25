package ca.kallistesoft.unarrow.core.designsystem

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/** The main action of a screen, as on kallistesoft.ca: accent fill, white bold text, radius 12, 52 tall. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        elevation = null,
        modifier = modifier.fillMaxWidth().heightIn(min = Sizes.buttonHeight),
    ) { ButtonText(text) }
}

/** A second action: the site's chip colours (soft accent fill), same size as the primary. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = UnarrowTheme.brand.chip,
            contentColor = UnarrowTheme.brand.onChip,
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = Sizes.buttonHeight),
    ) { ButtonText(text) }
}

/** A quiet action, the site's "ghost" button: transparent with a 1 px border. */
@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(Sizes.border, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = modifier.fillMaxWidth().heightIn(min = Sizes.buttonHeight),
    ) { ButtonText(text) }
}

@Composable
private fun ButtonText(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)

/**
 * A card as on the site: surface, 1 px border (or [borderOverride]), radius 18; clickable when [onClick] is set. Its
 * content keeps full colours when disabled.
 */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    padding: PaddingValues = PaddingValues(Spacing.xl),
    borderOverride: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    // A disabled card (today's daily, once solved) keeps its full colours: only its click is off.
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        disabledContentColor = MaterialTheme.colorScheme.onSurface,
    )
    val border = borderOverride ?: BorderStroke(Sizes.border, UnarrowTheme.brand.cardBorder)
    if (onClick != null) {
        Card(onClick, modifier, enabled, MaterialTheme.shapes.large, colors, border = border) {
            Box(Modifier.padding(padding)) { content() }
        }
    } else {
        Card(modifier, MaterialTheme.shapes.large, colors, border = border) { Box(Modifier.padding(padding)) { content() } }
    }
}

/** A small fact on the site's chip colours, fully rounded: a streak, a best result, a target. */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(color = UnarrowTheme.brand.chip, contentColor = UnarrowTheme.brand.onChip, shape = CircleShape, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(text, style = style, textAlign = TextAlign.Center)
        }
    }
}

/** A status pill with the site's small "ok" dot before [text]. */
@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
        border = BorderStroke(Sizes.border, UnarrowTheme.brand.cardBorder),
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(Sizes.dot).background(UnarrowTheme.brand.ok, CircleShape))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * The big call to action of a landing screen: a tall accent pill with a soft accent glow that, when [pulse], slowly
 * breathes (like the Neon games' play button, in Nocturne colours).
 */
@Composable
fun HeroButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, pulse: Boolean = true) {
    val glow = if (pulse) {
        val transition = rememberInfiniteTransition(label = "glow")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(GLOW_MILLIS), RepeatMode.Reverse),
            label = "glow",
        ).value
    } else {
        GLOW_STILL
    }
    val accent = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        elevation = null,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.heroButtonHeight)
            .shadow(
                elevation = Sizes.glowMin + (Sizes.glowMax - Sizes.glowMin) * glow,
                shape = CircleShape,
                ambientColor = accent,
                spotColor = accent,
            ),
    ) { Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) }
}

/** The player turned animations off (Developer options or Accessibility): screens stay still. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private const val GLOW_MILLIS = 1_800
private const val GLOW_STILL = 0.5f
