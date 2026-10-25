package ca.kallistesoft.unarrow.feature.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.kallistesoft.unarrow.core.designsystem.PrimaryButton
import ca.kallistesoft.unarrow.core.designsystem.SecondaryButton
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.Spacing
import ca.kallistesoft.unarrow.core.designsystem.TertiaryButton
import ca.kallistesoft.unarrow.core.designsystem.UnarrowPreviews
import ca.kallistesoft.unarrow.core.designsystem.PreviewSurface
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.engagement.PLAY_STORE_URL
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.ui.R
import kotlinx.coroutines.launch

/** A game: the puzzles of [mode], one after the other. [onBack] goes home. */
@Composable
fun GameScreen(
    mode: GameMode,
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel<GameViewModel, GameViewModel.Factory>(
        creationCallback = { factory -> factory.create(mode) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    GameContent(
        state,
        GameActions(
            onBack = onBack,
            onTap = viewModel::tap,
            onHint = viewModel::hint,
            onBuyHints = viewModel::buyHints,
            onRestart = viewModel::restart,
            onNext = viewModel::next,
            onHome = {
                scope.launch {
                    viewModel.leaveAfterWin()
                    onBack()
                }
            },
            onExtraLife = viewModel::extraLife,
            onShare = viewModel::share,
        ),
    )
}

/** What the game screen can do; one object so the content stays easy to preview and test. */
internal data class GameActions(
    val onBack: () -> Unit = {},
    val onTap: (Int) -> Unit = {},
    val onHint: () -> Unit = {},
    val onBuyHints: () -> Unit = {},
    val onRestart: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onExtraLife: () -> Unit = {},
    val onShare: (text: String, chooserTitle: String) -> Unit = { _, _ -> },
)

@Composable
internal fun GameContent(state: GameUiState, actions: GameActions, modifier: Modifier = Modifier) {
    val session = state.session
    Scaffold(
        modifier.testTag("game"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { GameTopBar(session, actions.onBack) },
    ) { padding ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Haptics(session, state.hapticsOn)
        Sounds(session, state.soundOn)
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = Spacing.screen, vertical = Spacing.l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                if (session.mode == GameMode.LEVELS && session.level <= TUTORIAL_LEVELS) {
                    Text(
                        stringResource(R.string.game_tutorial),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BoardView(session, actions.onTap, Modifier.widthIn(max = Sizes.maxContentWidth).fillMaxWidth())
                }
                Row(
                    Modifier.widthIn(max = Sizes.maxContentWidth).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    HintButton(state, actions, Modifier.weight(1f))
                    TertiaryButton(
                        stringResource(R.string.game_restart),
                        onClick = actions.onRestart,
                        modifier = Modifier.weight(1f),
                        enabled = session.status == GameStatus.PLAYING,
                    )
                }
            }
            AnimatedVisibility(
                visible = session.status != GameStatus.PLAYING,
                enter = slideInVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ResultPanel(state, actions)
            }
        }
    }
}

@Composable
private fun GameTopBar(session: GameSession?, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.s, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(
            when {
                session == null -> ""
                session.mode == GameMode.DAILY -> stringResource(R.string.game_daily_title, session.dailyNumber)
                else -> stringResource(R.string.game_level_title, session.level)
            },
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(start = Spacing.s),
        )
        if (session != null) Lives(session.lives)
    }
}

@Composable
private fun Lives(lives: Int) {
    val label = pluralStringResource(R.plurals.game_lives, lives, lives)
    Row(
        Modifier.padding(end = Spacing.m).semantics(mergeDescendants = true) { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        repeat(MAX_LIVES) { index ->
            Icon(
                if (index < lives) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = UnarrowTheme.gameColors.life,
                modifier = Modifier.size(Sizes.icon),
            )
        }
    }
}

/** A hint from the stock; with none left, one for a rewarded ad, else the hint pack for sale. */
@Composable
private fun HintButton(state: GameUiState, actions: GameActions, modifier: Modifier = Modifier) {
    val session = state.session
    val playing = session != null && session.status == GameStatus.PLAYING && session.hintId == null
    when {
        state.hints > 0 -> SecondaryButton(
            stringResource(R.string.game_hint) + " · " + state.hints,
            onClick = actions.onHint,
            modifier = modifier,
            enabled = playing,
        )
        state.rewardedReady -> SecondaryButton(
            stringResource(R.string.game_hint_ad),
            onClick = actions.onHint,
            modifier = modifier,
            enabled = playing,
        )
        state.hintPackPrice != null -> SecondaryButton(
            stringResource(R.string.game_get_hints) + " · " + state.hintPackPrice,
            onClick = actions.onBuyHints,
            modifier = modifier,
        )
        else -> SecondaryButton(stringResource(R.string.game_hint), onClick = {}, modifier = modifier, enabled = false)
    }
}

/** Solved or lost: what's next. */
@Composable
private fun ResultPanel(state: GameUiState, actions: GameActions) {
    val session = state.session ?: return
    Surface(
        Modifier.widthIn(max = Sizes.maxContentWidth).fillMaxWidth().testTag("result"),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = Spacing.s,
        shadowElevation = Spacing.s,
    ) {
        Column(
            Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (session.status == GameStatus.WON) {
                Text(stringResource(R.string.game_won), style = MaterialTheme.typography.headlineMedium)
                Text(
                    pluralStringResource(R.plurals.game_taps, session.taps, session.taps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (session.mode == GameMode.LEVELS) {
                    PrimaryButton(stringResource(R.string.game_next), onClick = actions.onNext)
                } else {
                    PrimaryButton(stringResource(R.string.game_home), onClick = actions.onHome)
                }
                val text = shareText(session)
                val title = stringResource(R.string.share_chooser)
                SecondaryButton(stringResource(R.string.game_share), onClick = { actions.onShare(text, title) })
            } else {
                Text(stringResource(R.string.game_lost), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.game_lost_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (state.rewardedReady) {
                    PrimaryButton(stringResource(R.string.game_extra_life), onClick = actions.onExtraLife)
                }
                SecondaryButton(stringResource(R.string.game_retry), onClick = actions.onRestart)
            }
        }
    }
}

/** "Unarrow Daily #12 ✅ ❤️❤️🤍 41 taps", then the Play Store link. */
@Composable
internal fun shareText(session: GameSession): String {
    val hearts = HEART.repeat(session.lives) + EMPTY_HEART.repeat(MAX_LIVES - session.lives)
    val summary = hearts + " " + pluralStringResource(R.plurals.game_taps, session.taps, session.taps)
    val result = if (session.mode == GameMode.DAILY) {
        stringResource(R.string.share_daily, session.dailyNumber, summary)
    } else {
        stringResource(R.string.share_level, session.level, summary)
    }
    return result + "\n" + stringResource(R.string.share_play, PLAY_STORE_URL)
}

/** A light buzz when an arrow is blocked, a confirmation when the puzzle is solved. */
@Composable
private fun Haptics(session: GameSession, enabled: Boolean) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(session.blocked?.serial) {
        if (enabled && session.blocked != null) haptics.performHapticFeedback(HapticFeedbackType.Reject)
    }
    LaunchedEffect(session.status) {
        if (enabled && session.status == GameStatus.WON) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
}

/** Levels that explain the rule above the board. */
internal const val TUTORIAL_LEVELS = 2

private const val HEART = "❤️"
private const val EMPTY_HEART = "🤍"

@UnarrowPreviews
@Composable
internal fun GamePreview() = PreviewSurface {
    val board = Levels.spec(14).board()
    GameContent(
        GameUiState(
            session = GameSession(GameMode.LEVELS, 14, 0, board, board.arrows.size, lives = 2, hintId = board.freeArrows().first().id),
            hints = 3,
        ),
        GameActions(),
    )
}
