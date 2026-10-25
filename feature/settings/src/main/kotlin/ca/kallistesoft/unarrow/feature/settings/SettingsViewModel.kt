package ca.kallistesoft.unarrow.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.kallistesoft.unarrow.core.ads.AdConsent
import ca.kallistesoft.unarrow.core.billing.Purchases
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val loading: Boolean = true,
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val privacyOptionsRequired: Boolean = false,
    /** "Purchases restored" shows once a restore finished. */
    val restored: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val progress: ProgressRepository,
    private val purchases: Purchases,
    private val consent: AdConsent,
) : ViewModel() {
    private val restored = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
        combine(progress.progress, consent.privacyOptionsRequired, restored) { p, privacy, restored ->
            SettingsUiState(
                loading = false,
                soundOn = p.soundOn,
                hapticsOn = p.hapticsOn,
                privacyOptionsRequired = privacy,
                restored = restored,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setSound(on: Boolean) {
        viewModelScope.launch { progress.setSound(on) }
    }

    fun setHaptics(on: Boolean) {
        viewModelScope.launch { progress.setHaptics(on) }
    }

    /** Asks Play again for what was bought (new phone, reinstall). */
    fun restorePurchases() {
        viewModelScope.launch {
            purchases.refresh()
            restored.value = true
        }
    }

    fun showPrivacyOptions() = consent.showPrivacyOptions()
}
