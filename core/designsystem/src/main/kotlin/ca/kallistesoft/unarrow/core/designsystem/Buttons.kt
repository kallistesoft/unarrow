package ca.kallistesoft.unarrow.core.designsystem

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** A card as on the site: surface, 1 px border, radius 18; clickable when [onClick] is set. */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    padding: PaddingValues = PaddingValues(Spacing.xl),
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    val border = BorderStroke(Sizes.border, UnarrowTheme.brand.cardBorder)
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
