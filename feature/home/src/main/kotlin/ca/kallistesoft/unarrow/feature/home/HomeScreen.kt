package ca.kallistesoft.unarrow.feature.home

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.kallistesoft.unarrow.core.designsystem.BrandCard
import ca.kallistesoft.unarrow.core.designsystem.Chip
import ca.kallistesoft.unarrow.core.designsystem.MonoLabel
import ca.kallistesoft.unarrow.core.designsystem.PreviewSurface
import ca.kallistesoft.unarrow.core.designsystem.PrimaryButton
import ca.kallistesoft.unarrow.core.designsystem.SecondaryButton
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.Spacing
import ca.kallistesoft.unarrow.core.designsystem.TertiaryButton
import ca.kallistesoft.unarrow.core.designsystem.UnarrowPreviews
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.ui.R

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state,
        onPlay,
        onDaily,
        onSettings,
        onRemoveAds = viewModel::buyRemoveAds,
        onRetryDaily = { viewModel.retryDaily(onDaily) },
    )
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onSettings: () -> Unit,
    onRemoveAds: () -> Unit,
    modifier: Modifier = Modifier,
    onRetryDaily: () -> Unit = {},
) {
    Scaffold(modifier.testTag("home"), contentWindowInsets = WindowInsets.safeDrawing) { padding ->
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
                Text(
                    stringResource(R.string.studio).uppercase(),
                    style = MonoLabel,
                    color = UnarrowTheme.brand.accentText,
                )
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
                Text(
                    stringResource(R.string.tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(Spacing.l))
                PrimaryButton(
                    stringResource(R.string.home_play) + " · " + stringResource(R.string.home_level, state.level),
                    onClick = onPlay,
                    enabled = !state.loading,
                )
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
            }
        }
    }
}

@Composable
private fun DailyCard(state: HomeUiState, onDaily: () -> Unit, onRetryDaily: () -> Unit) {
    // Once today's daily is solved the card only shows the result: one try per day, so the streak and the best daily
    // can't be replayed.
    BrandCard(
        onClick = onDaily,
        enabled = !state.loading && !state.dailyDone,
        modifier = Modifier.fillMaxWidth().testTag("daily"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_daily_number, state.dailyNumber),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (state.dailyDone) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = UnarrowTheme.brand.ok,
                    )
                }
            }
            Text(
                stringResource(if (state.dailyDone) R.string.home_daily_done else R.string.home_daily_new),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            if (!state.dailyDone) {
                SecondaryButton(stringResource(R.string.home_daily), onClick = onDaily, enabled = !state.loading)
            } else if (state.rewardedReady) {
                SecondaryButton(stringResource(R.string.home_daily_retry_ad), onClick = onRetryDaily)
            }
        }
    }
}

@UnarrowPreviews
@Composable
internal fun HomePreview() = PreviewSurface {
    HomeContent(
        HomeUiState(loading = false, level = 23, dailyNumber = 12, streak = 4, removeAdsPrice = "$3.99"),
        onPlay = {},
        onDaily = {},
        onSettings = {},
        onRemoveAds = {},
    )
}
