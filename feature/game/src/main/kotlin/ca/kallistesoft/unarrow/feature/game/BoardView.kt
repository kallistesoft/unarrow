package ca.kallistesoft.unarrow.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ca.kallistesoft.unarrow.core.designsystem.GameColors
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.game.Arrow
import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Direction
import ca.kallistesoft.unarrow.core.ui.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * The grid: a tile per cell and an arrow on each occupied one. The hinted arrow is green; a slide moves the arrow up to
 * what stops it (a shake when it already touches it) and flashes both red; a locked tap only shakes; rotating arrows
 * turn smoothly; arrows that leave slide out and fade; padlocks fade out when their key leaves. Each arrow also gets an
 * invisible touch target with a spoken label ("Arrow pointing up, blue lock"), so the board works with TalkBack and in
 * tests.
 */
@Composable
internal fun BoardView(session: GameSession, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = UnarrowTheme.gameColors
    val board = session.board
    val blocked = session.blocked
    val shake = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(blocked?.serial) {
        if (blocked == null) return@LaunchedEffect
        flash.snapTo(if (blocked.blockerId == null) 0f else 1f)
        shake.snapTo(if (blocked.distance == 0) 0f else 1f)
        shake.animateTo(1f, tween(SHAKE_MILLIS))
        flash.animateTo(0f, tween(FLASH_MILLIS))
    }
    val boardLabel = stringResource(R.string.game_board)

    BoxWithConstraints(
        modifier
            .aspectRatio(board.cols.toFloat() / board.rows)
            .semantics { contentDescription = boardLabel }
            .testTag("board"),
    ) {
        val cell = maxWidth / board.cols
        Canvas(Modifier.fillMaxSize()) {
            val cellPx = size.width / board.cols
            val gap = Sizes.cellGap.toPx()
            for (row in 0 until board.rows) {
                for (col in 0 until board.cols) {
                    drawRoundRect(
                        colors.tile,
                        topLeft = Offset(col * cellPx + gap / 2, row * cellPx + gap / 2),
                        size = Size(cellPx - gap, cellPx - gap),
                        cornerRadius = CornerRadius(cellPx * TILE_CORNER),
                    )
                }
            }
        }
        for (arrow in board.arrows) {
            key(arrow.id) {
                val involved = blocked != null && (arrow.id == blocked.arrowId || arrow.id == blocked.blockerId)
                val color = when {
                    arrow.id == session.hintId -> colors.hint
                    involved -> lerp(colors.arrow, colors.blocked, flash.value)
                    else -> colors.arrow
                }
                val nudge = if (arrow.id == blocked?.arrowId) shakeOffset(shake.value) else 0f
                PlacedArrow(arrow, board, color, nudge, colors)
            }
        }
        for (arrow in session.exits) {
            key(arrow.id) { ExitingArrow(arrow, board, colors.arrow) }
        }
        for (arrow in board.arrows) {
            key(arrow.id) {
                val label = arrowLabel(arrow, board)
                Box(
                    Modifier
                        .offset(cell * arrow.col, cell * arrow.row)
                        .size(cell)
                        .semantics { contentDescription = label }
                        .clickable { onTap(arrow.id) },
                )
            }
        }
    }
}

/** "Arrow pointing up", plus ", turning", ", blue lock" (while locked) or ", blue key". */
@Composable
private fun arrowLabel(arrow: Arrow, board: Board): String {
    val base = stringResource(R.string.game_arrow, stringResource(arrow.direction.label()))
    val lock = arrow.lock
    val key = arrow.key
    return when {
        arrow.rotating -> stringResource(R.string.game_arrow_rotating, base)
        lock != null && board.isLocked(arrow) ->
            stringResource(R.string.game_arrow_locked, base, stringResource(lockColourName(lock)))
        key != null -> stringResource(R.string.game_arrow_key, base, stringResource(lockColourName(key)))
        else -> base
    }
}

internal fun lockColourName(colour: Int): Int = when (colour) {
    0 -> R.string.lock_colour_0
    1 -> R.string.lock_colour_1
    else -> R.string.lock_colour_2
}

/**
 * An arrow on the board, animated as it changes: it glides to a new cell after a slide, turns a quarter at a time
 * (always clockwise) and its padlock fades once open. [nudge] (in cells) pushes it along its direction for a shake.
 */
@Composable
private fun PlacedArrow(arrow: Arrow, board: Board, color: Color, nudge: Float, colors: GameColors) {
    val row by animateFloatAsState(arrow.row.toFloat(), tween(SLIDE_MILLIS), label = "row")
    val col by animateFloatAsState(arrow.col.toFloat(), tween(SLIDE_MILLIS), label = "col")
    val turn = remember { Animatable(arrow.direction.degrees) }
    LaunchedEffect(arrow.direction) {
        val delta = Math.floorMod((arrow.direction.degrees - turn.value).toInt(), FULL_TURN)
        turn.animateTo(turn.value + delta, tween(TURN_MILLIS))
    }
    val locked by animateFloatAsState(if (board.isLocked(arrow)) 1f else 0f, tween(UNLOCK_MILLIS), label = "lock")
    Canvas(Modifier.fillMaxSize()) {
        val cellPx = size.width / board.cols
        val center = Offset((col + 0.5f) * cellPx, (row + 0.5f) * cellPx)
        drawArrow(center, turn.value, cellPx, color, nudge * cellPx)
        val badge = Offset(center.x + cellPx * BADGE_OFFSET, center.y - cellPx * BADGE_OFFSET)
        val key = arrow.key
        val lock = arrow.lock
        when {
            arrow.rotating -> drawTurnBadge(badge, cellPx, colors.arrow)
            key != null -> drawKey(badge, cellPx, colors.locks[key], key)
            lock != null && locked > 0f -> drawPadlock(badge, cellPx, colors.locks[lock].copy(alpha = locked), lock)
        }
    }
}

/** An arrow that just left: it slides to the edge it points to and fades out. */
@Composable
private fun ExitingArrow(arrow: Arrow, board: Board, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) }
    Canvas(Modifier.fillMaxSize()) {
        val cellPx = size.width / board.cols
        val travel = (board.exitDistance(arrow) + EXIT_OVERSHOOT) * cellPx * progress.value
        val center = Offset((arrow.col + 0.5f) * cellPx, (arrow.row + 0.5f) * cellPx)
        drawArrow(center, arrow.direction.degrees, cellPx, color.copy(alpha = 1f - progress.value), travel)
    }
}

/** Draws an arrow centred on [center], turned [degrees] from right, pushed [nudge] px along its direction. */
private fun DrawScope.drawArrow(center: Offset, degrees: Float, cellPx: Float, color: Color, nudge: Float) {
    rotate(degrees, pivot = center) {
        translate(left = nudge) {
            drawLine(
                color,
                start = Offset(center.x - cellPx * SHAFT_BACK, center.y),
                end = Offset(center.x + cellPx * SHAFT_FRONT, center.y),
                strokeWidth = cellPx * STROKE,
                cap = StrokeCap.Round,
            )
            val head = Path().apply {
                moveTo(center.x + cellPx * TIP, center.y)
                lineTo(center.x + cellPx * HEAD_BACK, center.y - cellPx * HEAD_HALF)
                lineTo(center.x + cellPx * HEAD_BACK, center.y + cellPx * HEAD_HALF)
                close()
            }
            drawPath(head, color)
        }
    }
}

/** ↻: an open circle with an arrowhead, in the corner of a rotating arrow's cell. */
private fun DrawScope.drawTurnBadge(center: Offset, cellPx: Float, color: Color) {
    val radius = cellPx * BADGE_RADIUS
    drawArc(
        color,
        startAngle = TURN_ARC_START,
        sweepAngle = TURN_ARC_SWEEP,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = cellPx * BADGE_STROKE, cap = StrokeCap.Round),
    )
    val head = Path().apply {
        moveTo(center.x + radius, center.y - radius * 0.9f)
        lineTo(center.x + radius * 1.5f, center.y + radius * 0.1f)
        lineTo(center.x + radius * 0.4f, center.y + radius * 0.1f)
        close()
    }
    drawPath(head, color)
}

/** A padlock: a shackle over a body whose shape tells the colour apart (round, square, diamond). */
private fun DrawScope.drawPadlock(center: Offset, cellPx: Float, color: Color, colour: Int) {
    val radius = cellPx * BADGE_RADIUS
    drawArc(
        color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.6f, center.y - radius * 1.2f),
        size = Size(radius * 1.2f, radius * 1.2f),
        style = Stroke(width = cellPx * BADGE_STROKE),
    )
    drawShape(Offset(center.x, center.y + radius * 0.3f), radius, color, colour)
}

/** A key: a head shaped like its lock's body (round, square, diamond) and a shaft with a bit. */
private fun DrawScope.drawKey(center: Offset, cellPx: Float, color: Color, colour: Int) {
    val radius = cellPx * BADGE_RADIUS
    val head = Offset(center.x - radius * 0.5f, center.y)
    drawLine(
        color,
        start = head,
        end = Offset(center.x + radius * 1.4f, center.y),
        strokeWidth = cellPx * BADGE_STROKE,
        cap = StrokeCap.Round,
    )
    drawLine(
        color,
        start = Offset(center.x + radius * 1.1f, center.y),
        end = Offset(center.x + radius * 1.1f, center.y + radius * 0.6f),
        strokeWidth = cellPx * BADGE_STROKE,
        cap = StrokeCap.Round,
    )
    drawShape(head, radius * 0.75f, color, colour)
}

private fun DrawScope.drawShape(center: Offset, radius: Float, color: Color, colour: Int) {
    when (colour) {
        0 -> drawCircle(color, radius * 0.8f, center)
        1 -> drawRect(color, Offset(center.x - radius * 0.75f, center.y - radius * 0.75f), Size(radius * 1.5f, radius * 1.5f))
        else -> drawPath(
            Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius, center.y)
                close()
            },
            color,
        )
    }
}

/** A bump towards the blocker that dies out: 0 at rest, in cells. */
internal fun shakeOffset(progress: Float): Float =
    (sin(progress * PI * SHAKE_BUMPS) * SHAKE_AMPLITUDE * (1 - progress)).toFloat()

internal fun Direction.label(): Int = when (this) {
    Direction.UP -> R.string.direction_up
    Direction.DOWN -> R.string.direction_down
    Direction.LEFT -> R.string.direction_left
    Direction.RIGHT -> R.string.direction_right
}

// Arrow shape, in cells from the centre of its cell (pointing right before rotation).
private const val SHAFT_BACK = 0.28f
private const val SHAFT_FRONT = 0.12f
private const val TIP = 0.32f
private const val HEAD_BACK = 0.06f
private const val HEAD_HALF = 0.2f
private const val STROKE = 0.12f
private const val TILE_CORNER = 0.2f

// Badges (rotation, lock, key), in the top right corner of the cell.
private const val BADGE_OFFSET = 0.31f
private const val BADGE_RADIUS = 0.13f
private const val BADGE_STROKE = 0.045f
private const val TURN_ARC_START = -60f
private const val TURN_ARC_SWEEP = 300f

private const val FULL_TURN = 360
private const val SHAKE_MILLIS = 320
private const val FLASH_MILLIS = 380
private const val SHAKE_BUMPS = 3
private const val SHAKE_AMPLITUDE = 0.12
private const val SLIDE_MILLIS = 220
private const val TURN_MILLIS = 240
private const val UNLOCK_MILLIS = 300
private const val EXIT_MILLIS = 260
private const val EXIT_OVERSHOOT = 0.5f
