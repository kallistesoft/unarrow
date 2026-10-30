package ca.kallistesoft.unarrow.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.kallistesoft.unarrow.core.data.AppTheme
import ca.kallistesoft.unarrow.core.designsystem.BrandCard
import ca.kallistesoft.unarrow.core.designsystem.MonoLabel
import ca.kallistesoft.unarrow.core.designsystem.PreviewSurface
import ca.kallistesoft.unarrow.core.designsystem.StatusPill
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.Spacing
import ca.kallistesoft.unarrow.core.designsystem.UnarrowPreviews
import ca.kallistesoft.unarrow.core.designsystem.topBar
import ca.kallistesoft.unarrow.core.engagement.PLAY_STORE_URL
import ca.kallistesoft.unarrow.core.ui.R

@Composable
fun SettingsScreen(onBack: () -> Unit, versionName: String, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var language by remember { mutableStateOf(AppLanguage.current()) }
    SettingsContent(
        state,
        SettingsActions(
            onBack = onBack,
            onSound = viewModel::setSound,
            onHaptics = viewModel::setHaptics,
            onTheme = viewModel::setTheme,
            onLanguage = {
                language = it
                it.select()
            },
            onRestore = viewModel::restorePurchases,
            onPrivacy = viewModel::showPrivacyOptions,
            onRate = { runCatching { uriHandler.openUri(PLAY_STORE_URL) } },
        ),
        language = language,
        versionName = versionName,
    )
}

internal data class SettingsActions(
    val onBack: () -> Unit = {},
    val onSound: (Boolean) -> Unit = {},
    val onHaptics: (Boolean) -> Unit = {},
    val onTheme: (AppTheme) -> Unit = {},
    val onLanguage: (AppLanguage) -> Unit = {},
    val onRestore: () -> Unit = {},
    val onPrivacy: () -> Unit = {},
    val onRate: () -> Unit = {},
)

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    actions: SettingsActions,
    language: AppLanguage,
    versionName: String,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier.testTag("settings"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.topBar).padding(Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = Sizes.maxContentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.m),
            ) {
                Section {
                    SwitchRow(stringResource(R.string.settings_sound), state.soundOn, actions.onSound)
                    SwitchRow(stringResource(R.string.settings_haptics), state.hapticsOn, actions.onHaptics)
                }

                SectionTitle(stringResource(R.string.settings_theme))
                Section {
                    for (option in AppTheme.entries) {
                        RadioRow(stringResource(option.label()), option == state.theme) { actions.onTheme(option) }
                    }
                }

                SectionTitle(stringResource(R.string.settings_language))
                Section {
                    for (option in AppLanguage.entries) {
                        RadioRow(stringResource(option.label), option == language) { actions.onLanguage(option) }
                    }
                }

                SectionTitle(stringResource(R.string.settings_purchases))
                Section {
                    LinkRow(stringResource(R.string.settings_restore), actions.onRestore)
                    if (state.restored) {
                        StatusPill(stringResource(R.string.settings_restored), Modifier.padding(bottom = Spacing.s))
                    }
                    if (state.privacyOptionsRequired) LinkRow(stringResource(R.string.settings_privacy), actions.onPrivacy)
                    LinkRow(stringResource(R.string.settings_rate), actions.onRate)
                }
                Text(
                    stringResource(R.string.settings_version, versionName),
                    style = MonoLabel,
                    color = UnarrowTheme.brand.muted2,
                    modifier = Modifier.padding(top = Spacing.l),
                )
            }
        }
    }
}

private fun AppTheme.label(): Int = when (this) {
    AppTheme.SYSTEM -> R.string.settings_theme_system
    AppTheme.LIGHT -> R.string.settings_theme_light
    AppTheme.DARK -> R.string.settings_theme_dark
}

/** A group of rows on a card, as on kallistesoft.ca. */
@Composable
private fun Section(content: @Composable () -> Unit) {
    BrandCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.xs)) {
        Column { content() }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = Spacing.m))
    }
}

@Composable
private fun SectionTitle(text: String) {
    // The site's section labels: small, mono, in the accent's text colour.
    Text(
        text.uppercase(),
        style = MonoLabel,
        color = UnarrowTheme.brand.accentText,
        modifier = Modifier.padding(top = Spacing.l, bottom = Spacing.s),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.m),
    )
}

@UnarrowPreviews
@Composable
internal fun SettingsPreview() = PreviewSurface {
    SettingsContent(
        SettingsUiState(loading = false, privacyOptionsRequired = true),
        SettingsActions(),
        language = AppLanguage.SYSTEM,
        versionName = "1.0.0",
    )
}
