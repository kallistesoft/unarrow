package ca.kallistesoft.unarrow.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ca.kallistesoft.unarrow.core.designsystem.GameColors
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.designsystem.rememberReducedMotion
import ca.kallistesoft.unarrow.core.game.Arrow
import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Direction
import ca.kallistesoft.unarrow.core.ui.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The board, as game pieces on a tray: empty cells are shallow holes; each arrow sits on a raised tile (lighter at the
 * top, a highlight edge, a soft shadow) and is coloured by its direction (up blue, right green, down amber, left coral),
 * its shape still saying the direction. Locked tiles take their lock's colour and a large padlock (round, square or
 * diamond); keys glow softly until used; rotating tiles wear an accent ring that spins as they turn.
 *
 * Moves: an exit zips out with a short trail and a burst at the edge; a slide glides, then shakes and flashes coral
 * (a shake alone when it already touched its blocker; a locked tap only shakes); the hint pulses an accent outline;
 * a win sends a ripple through the holes from the last exit. All of that is static when the system's animations are
 * off (and in previews). Each arrow also gets an invisible touch target with a spoken label ("Arrow pointing up, blue
 * lock"), so the board works with TalkBack and in tests. Drawing caches its brushes and paths per size.
 */
@Composable
internal fun BoardView(session: GameSession, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = UnarrowTheme.gameColors
    val accent = MaterialTheme.colorScheme.primary
    val motion = !LocalInspectionMode.current && !rememberReducedMotion()
    val board = session.board
    val blocked = session.blocked
    val shake = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(blocked?.serial) {
        if (blocked == null) return@LaunchedEffect
        flash.snapTo(if (blocked.blockerId == null) 0f else 1f)
        shake.snapTo(if (blocked.distance == 0 || !motion) 0f else 1f)
        if (motion) shake.animateTo(1f, tween(SHAKE_MILLIS)) else shake.snapTo(1f)
        flash.animateTo(0f, tween(FLASH_MILLIS))
    }
    val pulse = hintPulse(motion && session.hintId != null)
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(session.status) {
        if (session.status == GameStatus.WON && motion) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(RIPPLE_MILLIS))
        } else {
            ripple.snapTo(0f)
        }
    }
    val origin = session.exits.lastOrNull()
    val boardLabel = stringResource(R.string.game_board)

    BoxWithConstraints(
        modifier
            .aspectRatio(board.cols.toFloat() / board.rows)
            .semantics { contentDescription = boardLabel }
            .testTag("board"),
    ) {
        val cell = maxWidth / board.cols
        Holes(board, colors, accent, ripple, origin)
        for (arrow in board.arrows) {
            key(arrow.id) {
                val involved = blocked != null && (arrow.id == blocked.arrowId || arrow.id == blocked.blockerId)
                PlacedArrow(
                    arrow,
                    board,
                    colors,
                    accent,
                    motion,
                    flash = if (involved) flash.asState() else NO_FLASH,
                    shake = if (arrow.id == blocked?.arrowId) shake.asState() else NO_SHAKE,
                    hint = if (arrow.id == session.hintId) pulse else null,
                )
            }
        }
        for (arrow in session.exits) {
            key(arrow.id) { ExitingArrow(arrow, board, colors, motion) }
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

/** The hint's outline: pulsing while motion is allowed, steady otherwise. */
@Composable
private fun hintPulse(animate: Boolean): State<Float> =
    if (animate) {
        rememberInfiniteTransition(label = "hint").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
            label = "hint",
        )
    } else {
        remember { mutableFloatStateOf(STEADY_PULSE) }
    }

/** The tray's holes, shaded at the top; on a win a ripple runs through them from the last exit. */
@Composable
private fun Holes(board: Board, colors: GameColors, accent: Color, ripple: Animatable<Float, *>, origin: Arrow?) {
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val cellPx = size.width / board.cols
            val gap = Sizes.cellGap.toPx()
            val hole = cellPx - gap
            val corner = CornerRadius(hole * TILE_CORNER)
            val shade = Brush.verticalGradient(0f to colors.holeShade, HOLE_SHADE_DEPTH to Color.Transparent, endY = hole)
            val farthest = hypot(board.cols.toFloat(), board.rows.toFloat())
            onDrawBehind {
                drawRect(colors.tray)
                for (row in 0 until board.rows) {
                    for (col in 0 until board.cols) {
                        translate(col * cellPx + gap / 2, row * cellPx + gap / 2) {
                            drawRoundRect(colors.hole, size = Size(hole, hole), cornerRadius = corner)
                            drawRoundRect(shade, size = Size(hole, hole), cornerRadius = corner)
                            val wave = ripple.value
                            if (wave > 0f && origin != null) {
                                val distance = hypot((col - origin.col).toFloat(), (row - origin.row).toFloat()) / farthest
                                val glow = (1f - abs(wave * RIPPLE_REACH - distance) * RIPPLE_SHARPNESS).coerceAtLeast(0f)
                                if (glow > 0f) {
                                    drawRoundRect(accent.copy(alpha = glow * RIPPLE_ALPHA), size = Size(hole, hole), cornerRadius = corner)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

/** Brushes and paths of one piece, built once per size and direction. */
private class PieceCache(val tile: Float, val corner: CornerRadius, val body: Brush, val head: Path, val diamond: Path)

private fun pieceCache(tile: Float, top: Color, bottom: Color): PieceCache {
    val head = Path().apply {
        moveTo(tile * TIP, 0f)
        lineTo(tile * HEAD_BACK, -tile * HEAD_HALF)
        lineTo(tile * HEAD_BACK, tile * HEAD_HALF)
        close()
    }
    val diamond = Path().apply {
        moveTo(0f, -1f)
        lineTo(1f, 0f)
        lineTo(0f, 1f)
        lineTo(-1f, 0f)
        close()
    }
    return PieceCache(tile, CornerRadius(tile * TILE_CORNER), Brush.verticalGradient(listOf(top, bottom), endY = tile), head, diamond)
}

/**
 * An arrow on its tile, animated as it changes: it glides to a new cell after a slide, turns a quarter at a time
 * (always clockwise, its ring spinning), its padlock pops away when the key leaves; [flash] tints it coral, [shake]
 * bumps it towards its blocker, [hint] outlines it.
 */
@Composable
private fun PlacedArrow(
    arrow: Arrow,
    board: Board,
    colors: GameColors,
    accent: Color,
    motion: Boolean,
    flash: State<Float>,
    shake: State<Float>,
    hint: State<Float>?,
) {
    val row = animateFloatAsState(arrow.row.toFloat(), tween(SLIDE_MILLIS, easing = FastOutSlowInEasing), label = "row")
    val col = animateFloatAsState(arrow.col.toFloat(), tween(SLIDE_MILLIS, easing = FastOutSlowInEasing), label = "col")
    val turn = remember { Animatable(arrow.direction.degrees) }
    val spin = remember { Animatable(0f) }
    LaunchedEffect(arrow.direction) {
        val delta = Math.floorMod((arrow.direction.degrees - turn.value).toInt(), FULL_TURN)
        if (delta == 0) return@LaunchedEffect
        if (motion) {
            spin.snapTo(0f)
            spin.animateTo(1f, tween(TURN_MILLIS))
        }
        turn.animateTo(turn.value + delta, tween(TURN_MILLIS))
    }
    val locked = animateFloatAsState(if (board.isLocked(arrow)) 1f else 0f, tween(UNLOCK_MILLIS), label = "lock")
    val tint = colors.directions[arrow.direction.ordinal]
    val lock = arrow.lock
    val key = arrow.key
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val cellPx = size.width / board.cols
            val gap = Sizes.cellGap.toPx()
            val top = lerp(colors.tileTop, tint, TILE_TINT)
            val bottom = lerp(colors.tileBottom, tint, TILE_TINT)
            val cache = pieceCache(cellPx - gap, top, bottom)
            val tile = cache.tile
            val half = tile / 2
            val tileSize = Size(tile, tile)
            onDrawBehind {
                translate(col.value * cellPx + gap / 2, row.value * cellPx + gap / 2) {
                    // Shadow, body, the lock's colour, the highlight edge, a coral flash.
                    drawRoundRect(colors.pieceShadow, Offset(0f, tile * SHADOW_DROP), tileSize, cache.corner)
                    drawRoundRect(cache.body, size = tileSize, cornerRadius = cache.corner)
                    val lockedNow = locked.value
                    if (lock != null && lockedNow > 0f) {
                        drawRoundRect(colors.locks[lock].copy(alpha = LOCK_TINT * lockedNow), size = tileSize, cornerRadius = cache.corner)
                    }
                    drawLine(
                        colors.highlight,
                        Offset(cache.corner.x, Sizes.border.toPx()),
                        Offset(tile - cache.corner.x, Sizes.border.toPx()),
                        strokeWidth = Sizes.border.toPx(),
                    )
                    if (flash.value > 0f) {
                        drawRoundRect(colors.blocked.copy(alpha = flash.value * FLASH_ALPHA), size = tileSize, cornerRadius = cache.corner)
                    }
                    hint?.let { pulse ->
                        drawRoundRect(
                            accent.copy(alpha = HINT_MIN_ALPHA + (1 - HINT_MIN_ALPHA) * pulse.value),
                            size = tileSize,
                            cornerRadius = cache.corner,
                            style = Stroke(width = tile * (HINT_STROKE + HINT_STROKE_PULSE * pulse.value)),
                        )
                    }
                    if (arrow.rotating) drawRing(half, accent, spin.value)
                    // A locked arrow is drawn in the text colour, so the lock's colour is the only colour on its tile.
                    val glyph = when {
                        hint != null -> colors.hint
                        lock != null && lockedNow > LOCK_GLYPH_THRESHOLD -> colors.arrow
                        else -> tint
                    }
                    // With a key (top right) or a padlock (bottom right), the arrow shrinks a little and makes room.
                    val withKey = key != null
                    val withLock = lock != null && lockedNow > 0f
                    val shift = if (withKey || withLock) -tile * BADGED_SHIFT else 0f
                    translate(shift, if (withKey) -shift else shift) {
                        rotate(turn.value, pivot = Offset(half, half)) {
                            translate(half + shakeOffset(shake.value) * cellPx, half) {
                                scale(if (withKey || withLock) BADGED_GLYPH else 1f, pivot = Offset.Zero) { drawGlyph(cache, glyph) }
                            }
                        }
                    }
                    when {
                        key != null -> drawKey(Offset(tile * BADGE_X, tile * BADGE_Y), tile, colors.locks[key], key, cache)
                        lock != null && lockedNow > 0f -> drawPadlock(
                            Offset(tile * PADLOCK_X, tile * PADLOCK_Y),
                            tile,
                            colors.locks[lock].copy(alpha = lockedNow),
                            lock,
                            pop = 1f + (1f - lockedNow) * UNLOCK_POP,
                            cache,
                        )
                    }
                }
            }
        },
    )
}

/** An arrow that just left: it zips to the edge it points to with a short trail, fades, and bursts at the edge. */
@Composable
private fun ExitingArrow(arrow: Arrow, board: Board, colors: GameColors, motion: Boolean) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) }
    val tint = colors.directions[arrow.direction.ordinal]
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val cellPx = size.width / board.cols
            val gap = Sizes.cellGap.toPx()
            val cache = pieceCache(cellPx - gap, colors.tileTop, colors.tileBottom)
            val tile = cache.tile
            val half = tile / 2
            val distance = (board.exitDistance(arrow) + EXIT_OVERSHOOT) * cellPx
            val dx = arrow.direction.dCol.toFloat()
            val dy = arrow.direction.dRow.toFloat()
            onDrawBehind {
                val p = progress.value
                val startX = arrow.col * cellPx + gap / 2
                val startY = arrow.row * cellPx + gap / 2
                val travel = distance * p
                if (motion) {
                    // The trail, from where it started to where it is.
                    drawLine(
                        tint.copy(alpha = (1 - p) * TRAIL_ALPHA),
                        Offset(startX + half, startY + half),
                        Offset(startX + half + dx * travel, startY + half + dy * travel),
                        strokeWidth = tile * TRAIL_WIDTH,
                        cap = StrokeCap.Round,
                    )
                    if (p > BURST_START) burst(arrow, board, cellPx, tint, (p - BURST_START) / (1 - BURST_START))
                }
                translate(startX + dx * travel, startY + dy * travel) {
                    val alpha = 1f - p
                    drawRoundRect(colors.tileTop.copy(alpha = alpha), size = Size(tile, tile), cornerRadius = cache.corner)
                    rotate(arrow.direction.degrees, pivot = Offset(half, half)) {
                        translate(half, half) { drawGlyph(cache, tint.copy(alpha = alpha)) }
                    }
                }
            }
        },
    )
}

/** A few sparks flying out where the arrow crosses the edge of the board. */
private fun DrawScope.burst(arrow: Arrow, board: Board, cellPx: Float, color: Color, t: Float) {
    val edge = board.exitDistance(arrow) - EDGE_BACK
    val x = (arrow.col + 0.5f + arrow.direction.dCol * edge) * cellPx
    val y = (arrow.row + 0.5f + arrow.direction.dRow * edge) * cellPx
    val reach = cellPx * BURST_REACH * t
    for (i in 0 until SPARKS) {
        val angle = (arrow.direction.degrees + SPARK_SPREAD * (i - (SPARKS - 1) / 2f)) * PI.toFloat() / HALF_TURN
        drawCircle(
            color.copy(alpha = 1f - t),
            radius = cellPx * SPARK_RADIUS * (1f - t / 2),
            center = Offset(x + cos(angle) * reach, y + sin(angle) * reach),
        )
    }
}

/** The arrow at the origin, pointing right: a bold shaft and a head. */
private fun DrawScope.drawGlyph(cache: PieceCache, color: Color) {
    val tile = cache.tile
    drawLine(color, Offset(-tile * SHAFT_BACK, 0f), Offset(tile * SHAFT_FRONT, 0f), strokeWidth = tile * STROKE, cap = StrokeCap.Round)
    drawPath(cache.head, color)
}

/** The accent ring of a rotating tile: an open circle that spins once as the arrow turns. */
private fun DrawScope.drawRing(half: Float, accent: Color, spin: Float) {
    val radius = half * RING_RADIUS
    drawArc(
        accent,
        startAngle = RING_START + spin * FULL_TURN,
        sweepAngle = RING_SWEEP,
        useCenter = false,
        topLeft = Offset(half - radius, half - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = half * RING_STROKE, cap = StrokeCap.Round),
    )
}

/** A large padlock: a shackle over a body whose shape tells the colour apart (round, square, diamond). */
private fun DrawScope.drawPadlock(center: Offset, tile: Float, color: Color, colour: Int, pop: Float, cache: PieceCache) {
    val radius = tile * PADLOCK_RADIUS
    scale(pop, pivot = center) {
        drawArc(
            color,
            startAngle = HALF_TURN.toFloat(),
            sweepAngle = HALF_TURN.toFloat(),
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.6f, center.y - radius * 1.2f),
            size = Size(radius * 1.2f, radius * 1.2f),
            style = Stroke(width = tile * BADGE_STROKE),
        )
        drawShape(Offset(center.x, center.y + radius * 0.3f), radius, color, colour, cache)
    }
}

/**
 * A key with a soft, steady glow (steady, so an idle board draws no frames): a head shaped like its lock's body and a
 * shaft with a bit.
 */
private fun DrawScope.drawKey(center: Offset, tile: Float, color: Color, colour: Int, cache: PieceCache) {
    val radius = tile * BADGE_RADIUS
    drawCircle(color.copy(alpha = KEY_GLOW_ALPHA), radius * KEY_GLOW_SIZE, center)
    val head = Offset(center.x - radius * 0.5f, center.y)
    val stroke = tile * BADGE_STROKE
    drawLine(color, head, Offset(center.x + radius * 1.4f, center.y), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(
        color,
        Offset(center.x + radius * 1.1f, center.y),
        Offset(center.x + radius * 1.1f, center.y + radius * 0.6f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawShape(head, radius * 0.75f, color, colour, cache)
}

private fun DrawScope.drawShape(center: Offset, radius: Float, color: Color, colour: Int, cache: PieceCache) {
    when (colour) {
        0 -> drawCircle(color, radius * 0.8f, center)
        1 -> drawRect(color, Offset(center.x - radius * 0.75f, center.y - radius * 0.75f), Size(radius * 1.5f, radius * 1.5f))
        else -> translate(center.x, center.y) { scale(radius, pivot = Offset.Zero) { drawPath(cache.diamond, color) } }
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

/** A bump towards the blocker that dies out: 0 at rest, in cells. */
internal fun shakeOffset(progress: Float): Float =
    (sin(progress * PI * SHAKE_BUMPS) * SHAKE_AMPLITUDE * (1 - progress)).toFloat()

internal fun Direction.label(): Int = when (this) {
    Direction.UP -> R.string.direction_up
    Direction.DOWN -> R.string.direction_down
    Direction.LEFT -> R.string.direction_left
    Direction.RIGHT -> R.string.direction_right
}

private val NO_FLASH: State<Float> = mutableFloatStateOf(0f)
private val NO_SHAKE: State<Float> = mutableFloatStateOf(1f)

// Arrow shape, in tiles from the centre of its tile (pointing right before rotation).
private const val SHAFT_BACK = 0.3f
private const val SHAFT_FRONT = 0.12f
private const val TIP = 0.34f
private const val HEAD_BACK = 0.04f
private const val HEAD_HALF = 0.22f
private const val STROKE = 0.14f
private const val TILE_CORNER = 0.2f

// Depth and colour.
private const val TILE_TINT = 0.08f
private const val LOCK_TINT = 0.32f
private const val LOCK_GLYPH_THRESHOLD = 0.5f
private const val SHADOW_DROP = 0.07f
private const val HOLE_SHADE_DEPTH = 0.45f
private const val FLASH_ALPHA = 0.55f

// Badges: the key (top right), the padlock (bottom right, larger), the rotation ring.
private const val BADGE_X = 0.8f
private const val BADGE_Y = 0.2f
private const val BADGE_RADIUS = 0.12f
private const val BADGE_STROKE = 0.05f
private const val PADLOCK_X = 0.76f
private const val PADLOCK_Y = 0.78f
private const val PADLOCK_RADIUS = 0.15f
private const val BADGED_GLYPH = 0.78f
private const val BADGED_SHIFT = 0.07f
private const val UNLOCK_POP = 0.7f
private const val KEY_GLOW_ALPHA = 0.22f
private const val KEY_GLOW_SIZE = 2.2f
private const val RING_RADIUS = 0.94f
private const val RING_STROKE = 0.07f
private const val RING_START = -60f
private const val RING_SWEEP = 300f

// Hint outline.
private const val HINT_STROKE = 0.05f
private const val HINT_STROKE_PULSE = 0.04f
private const val HINT_MIN_ALPHA = 0.5f
private const val STEADY_PULSE = 1f

// Exit trail and burst, win ripple.
private const val TRAIL_ALPHA = 0.45f
private const val TRAIL_WIDTH = 0.28f
private const val BURST_START = 0.55f
private const val BURST_REACH = 0.5f
private const val SPARKS = 5
private const val SPARK_SPREAD = 28f
private const val SPARK_RADIUS = 0.06f
private const val EDGE_BACK = 0.5f
private const val RIPPLE_REACH = 1.3f
private const val RIPPLE_SHARPNESS = 4f
private const val RIPPLE_ALPHA = 0.35f

private const val HALF_TURN = 180
private const val FULL_TURN = 360
private const val SHAKE_MILLIS = 300
private const val FLASH_MILLIS = 360
private const val SHAKE_BUMPS = 3
private const val SHAKE_AMPLITUDE = 0.12
private const val SLIDE_MILLIS = 200
private const val TURN_MILLIS = 240
private const val UNLOCK_MILLIS = 300
private const val EXIT_MILLIS = 280
private const val EXIT_OVERSHOOT = 0.5f
private const val PULSE_MILLIS = 700
private const val RIPPLE_MILLIS = 600
