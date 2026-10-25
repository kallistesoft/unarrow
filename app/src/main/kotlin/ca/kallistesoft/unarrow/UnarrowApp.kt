package ca.kallistesoft.unarrow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ca.kallistesoft.unarrow.core.update.FlexibleUpdate
import ca.kallistesoft.unarrow.feature.game.GameMode
import ca.kallistesoft.unarrow.feature.game.GameScreen
import ca.kallistesoft.unarrow.feature.home.HomeScreen
import ca.kallistesoft.unarrow.feature.settings.SettingsScreen
import ca.kallistesoft.unarrow.navigation.Game
import ca.kallistesoft.unarrow.navigation.Home
import ca.kallistesoft.unarrow.navigation.Settings

/** App navigation (Navigation 3): Home, a game, Settings. Each entry gets its own saved state and ViewModels. */
@Composable
fun UnarrowApp(versionName: String) {
    val backStack = rememberNavBackStack(Home)
    val snackbar = remember { SnackbarHostState() }
    val back: () -> Unit = { backStack.removeLastOrNull() }
    val open: (NavKey) -> Unit = { key -> if (backStack.lastOrNull() != key) backStack.add(key) }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Home> {
                        HomeScreen(
                            onPlay = { open(Game(GameMode.LEVELS)) },
                            onDaily = { open(Game(GameMode.DAILY)) },
                            onSettings = { open(Settings) },
                        )
                    }
                    entry<Game> { key -> GameScreen(mode = key.mode, onBack = back) }
                    entry<Settings> { SettingsScreen(onBack = back, versionName = versionName) }
                },
            )
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    }
    FlexibleUpdate(snackbar)
}
