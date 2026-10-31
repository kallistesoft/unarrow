package ca.kallistesoft.unarrow.core.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import ca.kallistesoft.unarrow.core.ui.R
import ca.kallistesoft.unarrow.core.designsystem.UnarrowPreviews
import ca.kallistesoft.unarrow.core.designsystem.PreviewSurface
import ca.kallistesoft.unarrow.core.designsystem.PrimaryButton
import ca.kallistesoft.unarrow.core.designsystem.StatusColumn
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.Spacing
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.launch

/**
 * Blocking screen shown when this build is older than the minimum allowed version. "Update" starts Google Play's
 * immediate in-app update when Play offers one, and opens the Play Store listing otherwise (sideloaded or debug
 * builds, or Play not knowing about the update yet). Back leaves the app: it can't be used until updated.
 */
@Composable
fun ForceUpdateScreen(storePackage: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ForceUpdateScreen(storePackage, remember(context) { AppUpdateManagerFactory.create(context) }, modifier)
}

/** [manager] is Play's in-app updates; tests pass a `FakeAppUpdateManager`. */
@Composable
internal fun ForceUpdateScreen(storePackage: String, manager: AppUpdateManager, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The result doesn't matter: if the user cancels, this screen is still here.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    // An immediate update interrupted (app sent to background) must be resumed when the user comes back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch { manager.resumeImmediateUpdate(launcher) }
    }

    ForceUpdateContent(
        onUpdate = {
            scope.launch {
                if (!manager.startImmediateUpdate(launcher)) context.openStoreListing(storePackage)
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun ForceUpdateContent(onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier.testTag("force-update"), contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        // Scrolls in a short window (landscape, split screen): "Update now" is the only way on, it must stay reachable.
        StatusColumn(padding, spacing = Spacing.l) {
            Icon(
                Icons.Rounded.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconHero),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.force_update_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.force_update_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                stringResource(R.string.force_update_button),
                onClick = onUpdate,
                modifier = Modifier.fillMaxWidth().testTag("force-update-button"),
            )
        }
    }
}

@UnarrowPreviews
@Composable
internal fun ForceUpdatePreview() = PreviewSurface { ForceUpdateContent(onUpdate = {}) }

/** Starts Play's immediate update flow; false when Play has no update to offer for this install. */
private suspend fun AppUpdateManager.startImmediateUpdate(
    launcher: ActivityResultLauncher<IntentSenderRequest>,
): Boolean = runCatching {
    val info = requestAppUpdateInfo()
    val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
    available && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
        startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
}.getOrDefault(false) // Not installed from Play, or Play unavailable.

private suspend fun AppUpdateManager.resumeImmediateUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
    runCatching {
        val info = requestAppUpdateInfo()
        if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
        }
    }
}

private fun Context.openStoreListing(packageName: String) {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
    val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
    for (intent in listOf(market, web)) {
        try {
            if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
}
