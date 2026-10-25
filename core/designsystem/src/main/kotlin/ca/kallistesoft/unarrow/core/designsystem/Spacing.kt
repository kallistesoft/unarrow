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
    // The studio site's corners (kallistesoft.ca): chips 6, small buttons 10, buttons 12, cards 18, panels 24.
    val cornerXs = 6.dp
    val cornerS = 10.dp
    val cornerM = 12.dp
    val cornerL = 18.dp
    val cornerXl = 24.dp

    /** Card and button borders. */
    val border = 1.dp

    /** The "ok" dot of a status pill. */
    val dot = 8.dp

    val icon = 24.dp
    val iconHero = 72.dp
    val buttonHeight = 52.dp
    val smallButtonHeight = 44.dp
    val touchTarget = 48.dp

    /** Widest a board or a column of buttons gets (tablets, landscape). */
    val maxContentWidth = 560.dp

    /** Gap between two cells of the board. */
    val cellGap = 4.dp
}
