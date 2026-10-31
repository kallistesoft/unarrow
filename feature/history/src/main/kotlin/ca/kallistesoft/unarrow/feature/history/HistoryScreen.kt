package ca.kallistesoft.unarrow.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.kallistesoft.unarrow.core.designsystem.BrandCard
import ca.kallistesoft.unarrow.core.designsystem.MonoLabel
import ca.kallistesoft.unarrow.core.designsystem.PreviewSurface
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.SmallButton
import ca.kallistesoft.unarrow.core.designsystem.Spacing
import ca.kallistesoft.unarrow.core.designsystem.UnarrowPreviews
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.designsystem.topBar
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.ui.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Every solved level and daily with its best stars and time, in two tabs; each can be played again after a rewarded
 * ad ([onReplayLevel], [onReplayDaily] open it once the ad was watched to the end).
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onReplayLevel: (Int) -> Unit,
    onReplayDaily: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state,
        onBack = onBack,
        onTab = viewModel::selectTab,
        onReplayLevel = { level -> viewModel.replay { onReplayLevel(level) } },
        onReplayDaily = { day -> viewModel.replay { onReplayDaily(day) } },
    )
}

@Composable
internal fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit = {},
    onTab: (HistoryTab) -> Unit = {},
    onReplayLevel: (Int) -> Unit = {},
    onReplayDaily: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier.testTag("history"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.topBar)) {
                Row(Modifier.fillMaxWidth().padding(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(stringResource(R.string.history), style = MaterialTheme.typography.titleLarge)
                }
                PrimaryTabRow(selectedTabIndex = state.tab.ordinal, containerColor = MaterialTheme.colorScheme.background) {
                    Tab(
                        selected = state.tab == HistoryTab.LEVELS,
                        onClick = { onTab(HistoryTab.LEVELS) },
                        text = { Text(stringResource(R.string.history_levels)) },
                    )
                    Tab(
                        selected = state.tab == HistoryTab.DAILY,
                        onClick = { onTab(HistoryTab.DAILY) },
                        text = { Text(stringResource(R.string.history_daily)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            val empty = if (state.tab == HistoryTab.LEVELS) state.levels.isEmpty() else state.dailies.isEmpty()
            if (!state.loading && empty) {
                Text(
                    stringResource(if (state.tab == HistoryTab.LEVELS) R.string.history_empty_levels else R.string.history_empty_daily),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Spacing.xxl).testTag("empty"),
                )
                return@Box
            }
            val locale = LocalConfiguration.current.locales[0]
            val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
            LazyColumn(
                Modifier.widthIn(max = Sizes.maxContentWidth).fillMaxSize().testTag("history_list"),
                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                if (!state.rewardedReady) {
                    item {
                        Text(
                            stringResource(R.string.history_no_ad),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("no_ad"),
                        )
                    }
                }
                when (state.tab) {
                    HistoryTab.LEVELS -> items(state.levels, key = { it.level }) { entry ->
                        Entry(
                            title = stringResource(R.string.game_level_title, entry.level),
                            date = null,
                            stars = entry.stars,
                            seconds = entry.seconds,
                            canReplay = state.rewardedReady,
                            onReplay = { onReplayLevel(entry.level) },
                        )
                    }
                    HistoryTab.DAILY -> items(state.dailies, key = { it.day }) { entry ->
                        Entry(
                            title = stringResource(R.string.game_daily_title, entry.number),
                            date = dateFormat.format(LocalDate.ofEpochDay(entry.day)),
                            stars = entry.stars,
                            seconds = entry.seconds,
                            canReplay = state.rewardedReady,
                            onReplay = { onReplayDaily(entry.day) },
                        )
                    }
                }
            }
        }
    }
}

/** One solved puzzle: its name (and date), its best stars and time, and "Replay (ad)". */
@Composable
private fun Entry(title: String, date: String?, stars: Int, seconds: Int?, canReplay: Boolean, onReplay: () -> Unit) {
    BrandCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (date != null) {
                    Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val starsLabel = pluralStringResource(R.plurals.game_stars, stars, stars)
                Text(
                    STAR.repeat(stars) + EMPTY_STAR.repeat(MAX_STARS - stars),
                    style = MaterialTheme.typography.titleMedium,
                    color = UnarrowTheme.gameColors.star,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = starsLabel },
                )
                val time = if (seconds == null) {
                    stringResource(R.string.history_no_time)
                } else {
                    stringResource(R.string.game_time, seconds / SECONDS, seconds % SECONDS)
                }
                val timeLabel = if (seconds == null) {
                    stringResource(R.string.history_no_time_label)
                } else {
                    stringResource(R.string.game_time_label, time)
                }
                Text(
                    time,
                    style = MonoLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = timeLabel },
                )
            }
            SmallButton(stringResource(R.string.history_replay), onClick = onReplay, enabled = canReplay)
        }
    }
}

private const val STAR = "★"
private const val EMPTY_STAR = "☆"
private const val MAX_STARS = 3
private const val SECONDS = 60

@UnarrowPreviews
@Composable
internal fun HistoryPreview() = PreviewSurface {
    HistoryContent(
        HistoryUiState(
            loading = false,
            levels = (27 downTo 20).map { LevelEntry(it, 1 + it % 3, if (it > 22) 40 + it * 3 else null) },
            dailies = listOf(DailyEntry(Levels.FIRST_DAILY_EPOCH_DAY + 3, 4, 3, 152)),
            rewardedReady = true,
        ),
    )
}
