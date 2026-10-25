package ca.kallistesoft.unarrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.kallistesoft.unarrow.core.billing.Purchases
import ca.kallistesoft.unarrow.core.data.AppTheme
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.flags.FeatureFlags
import ca.kallistesoft.unarrow.core.update.ForceUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    /** Keeps the splash screen up until the first screen is known. */
    data object Loading : MainUiState

    /**
     * [updateRequired]: this build is below the minimum version, only the update screen is shown. [theme]: the look
     * chosen in Settings.
     */
    data class Ready(val updateRequired: Boolean, val theme: AppTheme = AppTheme.SYSTEM) : MainUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val forceUpdate: ForceUpdateRepository,
    private val featureFlags: FeatureFlags,
    private val purchases: Purchases,
    progress: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = combine(forceUpdate.updateRequired, progress.progress) { required, p ->
        MainUiState.Ready(updateRequired = required, theme = p.theme)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState.Loading)

    /** The app came to the foreground: the latest minimum version and flags, and what was bought (refunds too). */
    fun onForeground() {
        viewModelScope.launch { forceUpdate.refresh() }
        viewModelScope.launch { featureFlags.refresh() }
        viewModelScope.launch { purchases.refresh() }
    }
}
