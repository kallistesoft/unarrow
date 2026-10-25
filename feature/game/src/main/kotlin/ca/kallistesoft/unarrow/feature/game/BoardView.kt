package ca.kallistesoft.unarrow.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.game.Arrow
import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Direction
import ca.kallistesoft.unarrow.core.ui.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * The grid: a tile per cell and an arrow on each occupied one. The hinted arrow is green; a blocked tap shakes the
 * arrow towards what stops it and flashes both red; arrows that leave slide out and fade. Each arrow also gets an
 * invisible touch target with a spoken label ("Arrow pointing up"), so the board works with TalkBack and in tests.
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
        flash.snapTo(1f)
        shake.snapTo(0f)
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
            for (arrow in board.arrows) {
                val involved = blocked != null && (arrow.id == blocked.arrowId || arrow.id == blocked.blockerId)
                val color = when {
                    arrow.id == session.hintId -> colors.hint
                    involved -> lerp(colors.arrow, colors.blocked, flash.value)
                    else -> colors.arrow
                }
                val nudge = if (arrow.id == blocked?.arrowId) shakeOffset(shake.value) * cellPx else 0f
                drawArrow(arrow, cellPx, color, nudge)
            }
        }
        for (arrow in session.exits) {
            key(arrow.id) { ExitingArrow(arrow, board, colors.arrow) }
        }
        for (arrow in board.arrows) {
            key(arrow.id) {
                val label = stringResource(R.string.game_arrow, stringResource(arrow.direction.label()))
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

/** An arrow that just left: it slides to the edge it points to and fades out. */
@Composable
private fun ExitingArrow(arrow: Arrow, board: Board, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) }
    Canvas(Modifier.fillMaxSize()) {
        val cellPx = size.width / board.cols
        val travel = (board.exitDistance(arrow) + EXIT_OVERSHOOT) * cellPx * progress.value
        drawArrow(arrow, cellPx, color.copy(alpha = 1f - progress.value), travel)
    }
}

/** Draws [arrow] in its cell, pushed [nudge] px along its direction. */
private fun DrawScope.drawArrow(arrow: Arrow, cellPx: Float, color: Color, nudge: Float) {
    val center = Offset((arrow.col + 0.5f) * cellPx, (arrow.row + 0.5f) * cellPx)
    rotate(arrow.direction.degrees, pivot = center) {
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

private const val SHAKE_MILLIS = 320
private const val FLASH_MILLIS = 380
private const val SHAKE_BUMPS = 3
private const val SHAKE_AMPLITUDE = 0.12
private const val EXIT_MILLIS = 260
private const val EXIT_OVERSHOOT = 0.5f
