package ca.kallistesoft.unarrow.core.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ca.kallistesoft.unarrow.core.ui.R
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo

/**
 * Play's flexible in-app update: when a new version has been out for [STALE_DAYS] days, Play downloads it while the
 * player keeps playing, then [snackbar] offers "Restart" to install it. Versions too old to be used at all get the
 * blocking [ForceUpdateScreen] instead.
 */
@Composable
fun FlexibleUpdate(snackbar: SnackbarHostState) {
    val context = LocalContext.current
    FlexibleUpdate(snackbar, remember(context) { AppUpdateManagerFactory.create(context) })
}

/** [manager] is Play's in-app updates; tests pass a `FakeAppUpdateManager`. */
@Composable
internal fun FlexibleUpdate(snackbar: SnackbarHostState, manager: AppUpdateManager) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    var downloaded by remember { mutableStateOf(false) }
    val message = stringResource(R.string.update_ready)
    val restart = stringResource(R.string.update_restart)

    DisposableEffect(manager) {
        val listener = InstallStateUpdatedListener { if (it.installStatus() == InstallStatus.DOWNLOADED) downloaded = true }
        manager.registerListener(listener)
        onDispose { manager.unregisterListener(listener) }
    }
    LaunchedEffect(manager) {
        runCatching {
            val info = manager.requestAppUpdateInfo()
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                downloaded = true
            } else if (
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                (info.clientVersionStalenessDays() ?: 0) >= STALE_DAYS
            ) {
                manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE))
            }
        } // Not installed from Play, or Play unavailable: no update offered.
    }
    LaunchedEffect(downloaded) {
        if (!downloaded) return@LaunchedEffect
        val result = snackbar.showSnackbar(message, actionLabel = restart, duration = SnackbarDuration.Indefinite)
        if (result == SnackbarResult.ActionPerformed) manager.completeUpdate()
    }
}

/** Days a new version waits before the flexible update is offered: most players get it from the Store anyway. */
const val STALE_DAYS = 3
