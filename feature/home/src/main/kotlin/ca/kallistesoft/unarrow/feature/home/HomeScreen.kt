package ca.kallistesoft.unarrow.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwipeRightAlt
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.kallistesoft.unarrow.core.data.DailyResult
import ca.kallistesoft.unarrow.core.designsystem.BrandCard
import ca.kallistesoft.unarrow.core.designsystem.Chip
import ca.kallistesoft.unarrow.core.designsystem.HeroButton
import ca.kallistesoft.unarrow.core.designsystem.MonoLabel
import ca.kallistesoft.unarrow.core.designsystem.PreviewSurface
import ca.kallistesoft.unarrow.core.designsystem.SecondaryButton
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.Spacing
import ca.kallistesoft.unarrow.core.designsystem.TertiaryButton
import ca.kallistesoft.unarrow.core.designsystem.UnarrowPreviews
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.designsystem.rememberReducedMotion
import ca.kallistesoft.unarrow.core.ui.R
import kotlin.math.PI
import kotlin.math.sin

/** The website opened from the footer. */
internal const val STUDIO_URL = "https://kallistesoft.ca"

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    HomeContent(
        state,
        onPlay,
        onDaily,
        onSettings,
        onRemoveAds = viewModel::buyRemoveAds,
        onRetryDaily = { viewModel.retryDaily(onDaily) },
        onSound = viewModel::toggleSound,
        onWebsite = { runCatching { uriHandler.openUri(STUDIO_URL) } },
    )
}

/**
 * The landing screen, in the family of the studio's Neon games but in Nocturne colours: a gradient with a few arrows
 * drifting behind, a two-tone glowing title, a glowing Play pill, the player's records, today's daily, how to play,
 * and a footer with the sound and the studio's site. Motion (entrance, drift, glow) is off when the system's
 * animations are, and in previews and screenshots.
 */
@Composable
internal fun HomeContent(
    state: HomeUiState,
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onSettings: () -> Unit,
    onRemoveAds: () -> Unit,
    modifier: Modifier = Modifier,
    onRetryDaily: () -> Unit = {},
    onSound: () -> Unit = {},
    onWebsite: () -> Unit = {},
) {
    val motion = !LocalInspectionMode.current && !rememberReducedMotion()
    val gradient = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, UnarrowTheme.brand.gradientEnd))
    Scaffold(
        modifier.testTag("home"),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(Modifier.fillMaxSize().background(gradient)) {
            DriftingArrows(motion)
            Box(
                Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier
                        .widthIn(max = Sizes.maxContentWidth)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.screen, vertical = Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.l),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                    Entrance(0, motion) {
                        Text(
                            stringResource(R.string.studio).uppercase(),
                            style = MonoLabel,
                            color = UnarrowTheme.brand.accentText,
                        )
                    }
                    Entrance(1, motion) { Title() }
                    Entrance(2, motion) {
                        Text(
                            stringResource(R.string.tagline),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Entrance(3, motion) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                            HeroButton(
                                stringResource(R.string.home_play) + " · " + stringResource(R.string.home_level, state.level),
                                onClick = onPlay,
                                enabled = !state.loading,
                                pulse = motion,
                                modifier = Modifier.padding(top = Spacing.m),
                            )
                            Records(state)
                        }
                    }
                    Entrance(4, motion) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                            DailyCard(state, onDaily, onRetryDaily)
                            when {
                                state.removeAdsOwned -> Text(
                                    stringResource(R.string.home_ads_removed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                                state.removeAdsPrice != null -> TertiaryButton(
                                    stringResource(R.string.home_remove_ads) + " · " + state.removeAdsPrice,
                                    onClick = onRemoveAds,
                                )
                            }
                            HowToPlay()
                            Footer(state.soundOn, onSound, onWebsite)
                        }
                    }
                }
            }
        }
    }
}

/** Fades and slides in, each [step] a little after the one before (like the site's .rv/.d1–.d4). */
@Composable
private fun Entrance(step: Int, motion: Boolean, content: @Composable () -> Unit) {
    val progress = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (motion) progress.animateTo(1f, tween(ENTRANCE_MILLIS, delayMillis = step * ENTRANCE_STEP_MILLIS))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1 - progress.value) * ENTRANCE_RISE * density
        },
    ) { content() }
}

/** "UNARROW" in two colours with a soft glow; the Dev build's name ("Unarrow Dev") keeps its marker. */
@Composable
private fun Title() {
    val name = stringResource(R.string.app_name)
    val first = name.take(TITLE_SPLIT).uppercase()
    val rest = name.drop(TITLE_SPLIT).uppercase()
    val accent = MaterialTheme.colorScheme.primary
    val second = UnarrowTheme.brand.titleSecond
    val style = MaterialTheme.typography.displayMedium
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = accent, shadow = Shadow(accent.copy(alpha = GLOW_ALPHA), blurRadius = GLOW_BLUR))) {
                append(first)
            }
            withStyle(SpanStyle(color = second, shadow = Shadow(second.copy(alpha = GLOW_ALPHA / 2), blurRadius = GLOW_BLUR))) {
                append(rest)
            }
        },
        style = style,
        modifier = Modifier.semantics { contentDescription = name },
    )
}

/** The player's records: highest level, every star earned, the longest daily streak. */
@Composable
private fun Records(state: HomeUiState) {
    Row(Modifier.fillMaxWidth().testTag("records"), horizontalArrangement = Arrangement.SpaceEvenly) {
        Record(state.level, R.string.home_record_level)
        Record(state.totalStars, R.string.home_record_stars)
        Record(state.bestStreak, R.string.home_record_streak)
    }
}

@Composable
private fun Record(value: Int, label: Int) {
    val name = stringResource(label)
    val spoken = stringResource(R.string.home_record_label, name, value)
    Column(
        Modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(name.uppercase(), style = MonoLabel, color = UnarrowTheme.brand.muted2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DailyCard(state: HomeUiState, onDaily: () -> Unit, onRetryDaily: () -> Unit) {
    // Once today's daily is solved the card only shows the result: one try per day, so the streak and the best daily
    // can't be replayed. It keeps its full colours (only its click is off) and an accent-to-amber edge.
    val edge = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, UnarrowTheme.gameColors.star))
    BrandCard(
        onClick = onDaily,
        enabled = !state.loading && !state.dailyDone,
        borderOverride = BorderStroke(Sizes.border * 2, edge),
        modifier = Modifier.fillMaxWidth().testTag("daily"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_daily_number, state.dailyNumber),
                    style = MaterialTheme.typography.titleLarge.merge(TextStyle(fontFamily = MonoLabel.fontFamily)),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).testTag("daily_title"),
                )
                if (state.dailyDone) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = UnarrowTheme.brand.ok)
                }
            }
            Text(
                stringResource(if (state.dailyDone) R.string.home_daily_done else R.string.home_daily_new),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (state.streak > 0) {
                    Chip(pluralStringResource(R.plurals.home_streak, state.streak, state.streak)) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = UnarrowTheme.gameColors.streak,
                            modifier = Modifier.size(Sizes.icon),
                        )
                    }
                }
                state.dailyResult?.let { Chip(resultText(it), Modifier.testTag("daily_result")) }
            }
            if (!state.dailyDone) {
                SecondaryButton(stringResource(R.string.home_daily), onClick = onDaily, enabled = !state.loading)
            } else if (state.rewardedReady) {
                SecondaryButton(stringResource(R.string.home_daily_retry_ad), onClick = onRetryDaily)
            }
        }
    }
}

/** "★★☆ 0:42". */
@Composable
private fun resultText(result: DailyResult): String =
    STAR.repeat(result.stars) + EMPTY_STAR.repeat(MAX_STARS - result.stars) + " " +
        stringResource(R.string.game_time, result.seconds / SECONDS, result.seconds % SECONDS)

/** The rules in four lines, each with a chip in the board's own colours. */
@Composable
private fun HowToPlay() {
    val game = UnarrowTheme.gameColors
    BrandCard(Modifier.fillMaxWidth().testTag("how_to_play")) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(
                stringResource(R.string.home_howto).uppercase(),
                style = MonoLabel,
                color = UnarrowTheme.brand.accentText,
            )
            Text(
                stringResource(R.string.home_howto_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Rule(listOf(Icons.AutoMirrored.Rounded.ArrowForward to game.hint), R.string.home_howto_exit)
            Rule(listOf(Icons.Rounded.SwipeRightAlt to game.life), R.string.home_howto_slide)
            Rule(listOf(Icons.Rounded.RotateRight to MaterialTheme.colorScheme.primary), R.string.home_howto_rotate)
            Rule(
                listOf(Icons.Rounded.Lock to game.locks[0], Icons.Rounded.Key to game.locks[0]),
                R.string.home_howto_lock,
            )
        }
    }
}

@Composable
private fun Rule(glyphs: List<Pair<ImageVector, Color>>, text: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            for ((icon, color) in glyphs) {
                Surface(
                    color = UnarrowTheme.gameColors.tile,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(Sizes.chip),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(Sizes.chipIcon))
                    }
                }
            }
        }
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Footer(soundOn: Boolean, onSound: () -> Unit, onWebsite: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onSound) {
            Icon(
                if (soundOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                contentDescription = stringResource(if (soundOn) R.string.home_sound_on else R.string.home_sound_off),
                tint = UnarrowTheme.brand.muted2,
            )
        }
        TextButton(onClick = onWebsite) {
            Text(stringResource(R.string.home_website), style = MonoLabel, color = UnarrowTheme.brand.muted2)
        }
    }
}

/**
 * A few arrows drifting and slowly turning behind the content, at very low contrast so they never get in the way of
 * the text. Still (but drawn) when [motion] is off.
 */
@Composable
private fun DriftingArrows(motion: Boolean) {
    val time = if (motion) {
        rememberInfiniteTransition(label = "drift").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(DRIFT_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "drift",
        ).value
    } else {
        0f
    }
    val color = MaterialTheme.colorScheme.onBackground.copy(alpha = DRIFT_ALPHA)
    Canvas(Modifier.fillMaxSize().testTag("drift")) {
        for ((index, spot) in DRIFT_SPOTS.withIndex()) {
            val phase = (time + index / DRIFT_SPOTS.size.toFloat()) * 2 * PI
            val center = Offset(
                size.width * spot.x + sin(phase).toFloat() * size.width * DRIFT_RANGE,
                size.height * spot.y + sin(phase * 2).toFloat() * size.height * DRIFT_RANGE / 2,
            )
            val length = size.width * spot.size
            rotate(spot.degrees + time * FULL_TURN * if (index % 2 == 0) 1 else -1, pivot = center) {
                drawLine(color, Offset(center.x - length / 2, center.y), Offset(center.x + length / 4, center.y),
                    strokeWidth = length / 5, cap = StrokeCap.Round)
                drawPath(
                    Path().apply {
                        moveTo(center.x + length / 2, center.y)
                        lineTo(center.x + length / 6, center.y - length / 3)
                        lineTo(center.x + length / 6, center.y + length / 3)
                        close()
                    },
                    color,
                )
            }
        }
    }
}

/** Where the background arrows sit (fractions of the screen), their size (of its width) and first angle. */
private class Spot(val x: Float, val y: Float, val size: Float, val degrees: Float)

private val DRIFT_SPOTS = listOf(
    Spot(0.15f, 0.12f, 0.16f, 0f),
    Spot(0.85f, 0.28f, 0.22f, 90f),
    Spot(0.1f, 0.55f, 0.2f, 270f),
    Spot(0.9f, 0.7f, 0.14f, 180f),
    Spot(0.5f, 0.9f, 0.24f, 0f),
)

private const val TITLE_SPLIT = 2
private const val GLOW_ALPHA = 0.55f
private const val GLOW_BLUR = 36f
private const val ENTRANCE_MILLIS = 700
private const val ENTRANCE_STEP_MILLIS = 100
private const val ENTRANCE_RISE = 28f
private const val DRIFT_MILLIS = 60_000
private const val DRIFT_ALPHA = 0.06f
private const val DRIFT_RANGE = 0.04f
private const val FULL_TURN = 360f
private const val STAR = "★"
private const val EMPTY_STAR = "☆"
private const val MAX_STARS = 3
private const val SECONDS = 60

@UnarrowPreviews
@Composable
internal fun HomePreview() = PreviewSurface {
    HomeContent(
        HomeUiState(
            loading = false, level = 23, dailyNumber = 12, streak = 4, removeAdsPrice = "$3.99", totalStars = 58,
            bestStreak = 9,
        ),
        onPlay = {},
        onDaily = {},
        onSettings = {},
        onRemoveAds = {},
    )
}
