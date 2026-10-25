package ca.kallistesoft.unarrow

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import ca.kallistesoft.unarrow.core.data.AppTheme
import ca.kallistesoft.unarrow.core.testing.FakeFeatureFlags
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakePurchases
import ca.kallistesoft.unarrow.core.testing.MainDispatcherRule
import ca.kallistesoft.unarrow.core.testing.collecting
import ca.kallistesoft.unarrow.core.update.ForceUpdateRepository
import ca.kallistesoft.unarrow.core.update.ForceUpdateSettings
import ca.kallistesoft.unarrow.core.update.MinVersionSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MainActivityViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `an outdated build only gets the update screen, and coming back refreshes everything`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.root.resolve("u.preferences_pb") }
        val forceUpdate = ForceUpdateRepository(
            store,
            MinVersionSource { 5L },
            ForceUpdateSettings("min_version_code", currentVersionCode = 3, storePackage = "ca.example"),
        )
        val purchases = FakePurchases()
        val progress = FakeProgressRepository()
        val vm = MainActivityViewModel(forceUpdate, FakeFeatureFlags(), purchases, progress)
        collecting(vm.uiState)
        assertEquals(MainUiState.Ready(updateRequired = false), vm.uiState.first { it is MainUiState.Ready })

        vm.onForeground()
        assertEquals(MainUiState.Ready(updateRequired = true), vm.uiState.first { it == MainUiState.Ready(true) })
        assertEquals(1, purchases.refreshes)

        progress.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, (vm.uiState.first { (it as MainUiState.Ready).theme == AppTheme.DARK } as MainUiState.Ready).theme)
    }
}
