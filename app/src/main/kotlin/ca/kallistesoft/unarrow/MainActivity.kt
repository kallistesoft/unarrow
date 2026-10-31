package ca.kallistesoft.unarrow

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import ca.kallistesoft.unarrow.core.data.AppTheme
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ca.kallistesoft.unarrow.core.ads.AdConsent
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.update.ForceUpdateScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Single activity. AppCompat only provides the per-app language API; the UI is all Compose. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    @Inject lateinit var adConsent: AdConsent

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge()
        // Ads consent (UMP) on each launch: the form shows only where it's required; ads start once allowed.
        adConsent.gatherConsent(this)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForeground() }
            val theme = (state as? MainUiState.Ready)?.theme ?: AppTheme.SYSTEM
            val dark = when (theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            // System bar icons follow the chosen look, not only the system's.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            UnarrowTheme(darkTheme = dark) {
                when (val ready = state) {
                    MainUiState.Loading -> Unit
                    is MainUiState.Ready ->
                        if (ready.updateRequired) {
                            ForceUpdateScreen(storePackage = BuildConfig.STORE_PACKAGE)
                        } else {
                            UnarrowApp(versionName = BuildConfig.VERSION_NAME)
                        }
                }
            }
        }
    }
}
