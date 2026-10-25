package ca.kallistesoft.unarrow.core.designsystem

import androidx.compose.ui.unit.dp

/** Spacing scale used by every screen, so gaps stay consistent. The only place, with [Sizes], with dp values. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Side margin of a screen's content. */
    val screen = 20.dp
}

/** Component sizes. */
object Sizes {
    val cornerXs = 6.dp
    val cornerS = 12.dp
    val cornerM = 16.dp
    val cornerL = 24.dp
    val cornerXl = 32.dp

    val icon = 24.dp
    val iconHero = 72.dp
    val buttonHeight = 56.dp
    val touchTarget = 48.dp

    /** Widest a board or a column of buttons gets (tablets, landscape). */
    val maxContentWidth = 560.dp

    /** Gap between two cells of the board. */
    val cellGap = 4.dp
}
