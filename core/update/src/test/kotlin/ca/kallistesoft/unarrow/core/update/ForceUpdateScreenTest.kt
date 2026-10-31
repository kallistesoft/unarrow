package ca.kallistesoft.unarrow.core.update

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * "Update" with Play's in-app updates faked, and without Play at all (Robolectric, like a sideloaded build), where it
 * opens the store listing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK], qualifiers = ScreenshotDevice.LIGHT_EN)
class ForceUpdateScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `update opens the Play Store listing when Play offers no in-app update`() {
        compose.setContent { ForceUpdateScreen(storePackage = "ca.example.hunt") }
        compose.onNodeWithText("Update").performClick()

        val intent = waitForStartedActivity()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("market://details?id=ca.example.hunt", intent.dataString)
    }

    @Test
    fun `without a store app, the listing opens in the browser`() {
        val web = "https://play.google.com/store/apps/details?id=ca.example.hunt"
        installOnlyBrowserFor(web)
        compose.setContent { ForceUpdateScreen(storePackage = "ca.example.hunt") }
        compose.onNodeWithText("Update").performClick()

        assertEquals(web, waitForStartedActivity().dataString)
    }

    @Test
    fun `update starts Play's immediate update when Play offers one`() {
        val play = FakeAppUpdateManager(compose.activity).apply { setUpdateAvailable(2, AppUpdateType.IMMEDIATE) }
        compose.setContent { ForceUpdateScreen("ca.example.hunt", play) }
        compose.onNodeWithText("Update").performClick()

        compose.waitUntil(5_000) { play.isImmediateFlowVisible }
        assertNull("no store listing", shadowOf(compose.activity).nextStartedActivity)
    }

    @Test
    fun `an update Play only allows in the background opens the store listing`() {
        val play = FakeAppUpdateManager(compose.activity).apply { setUpdateAvailable(2, AppUpdateType.FLEXIBLE) }
        compose.setContent { ForceUpdateScreen("ca.example.hunt", play) }
        compose.onNodeWithText("Update").performClick()

        assertEquals("market://details?id=ca.example.hunt", waitForStartedActivity().dataString)
        assertFalse(play.isImmediateFlowVisible)
    }

    @Test
    fun `an interrupted update resumes when the user comes back`() {
        var flowsStarted = 0
        val play = object : FakeAppUpdateManager(compose.activity) {
            override fun startUpdateFlowForResult(
                info: AppUpdateInfo,
                launcher: ActivityResultLauncher<IntentSenderRequest>,
                options: AppUpdateOptions,
            ): Boolean = super.startUpdateFlowForResult(info, launcher, options).also { flowsStarted++ }
        }
        play.setUpdateAvailable(2, AppUpdateType.IMMEDIATE)
        compose.setContent { ForceUpdateScreen("ca.example.hunt", play) }
        compose.waitForIdle()
        assertEquals("nothing to resume yet", 0, flowsStarted)

        compose.onNodeWithText("Update").performClick()
        compose.waitUntil(5_000) { flowsStarted == 1 }
        play.userAcceptsUpdate()
        play.downloadStarts()

        // The app goes to the background mid-update, then comes back.
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) { flowsStarted == 2 }
    }

    /** Only [url] can be opened: the market:// link fails like on a device without the Play Store. */
    private fun installOnlyBrowserFor(url: String) {
        val app = compose.activity.application
        shadowOf(app).checkActivities(true)
        val browser = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.example.browser"
                name = "Browser"
                applicationInfo = ApplicationInfo().apply { packageName = "com.example.browser" }
            }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(Intent.ACTION_VIEW, url.toUri()), browser)
    }

    private fun waitForStartedActivity(): Intent {
        val activity = shadowOf(compose.activity)
        compose.waitUntil(5_000) { activity.peekNextStartedActivity() != null }
        return activity.nextStartedActivity
    }
}
