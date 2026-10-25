package ca.kallistesoft.unarrow.core.update

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.robolectric.RuntimeEnvironment
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK], qualifiers = ScreenshotDevice.LIGHT_EN)
class FlexibleUpdateTest {
    @get:Rule
    val compose = createComposeRule()

    private val manager = FakeAppUpdateManager(RuntimeEnvironment.getApplication())

    private fun show() {
        val snackbar = SnackbarHostState()
        compose.setContent {
            FlexibleUpdate(snackbar, manager)
            SnackbarHost(snackbar)
        }
        compose.waitForIdle()
    }

    @Test
    fun `a stale update downloads in the background, then Restart installs it`() {
        manager.setUpdateAvailable(2, AppUpdateType.FLEXIBLE)
        manager.setClientVersionStalenessDays(STALE_DAYS)
        show()
        assertTrue(manager.isConfirmationDialogVisible)
        manager.userAcceptsUpdate()
        manager.downloadStarts()
        manager.downloadCompletes()
        compose.waitForIdle()
        compose.onNodeWithText("Restart").performClick()
        compose.waitForIdle()
        assertTrue(manager.isInstallSplashScreenVisible)
    }

    @Test
    fun `a fresh update isn't offered yet`() {
        manager.setUpdateAvailable(2, AppUpdateType.FLEXIBLE)
        manager.setClientVersionStalenessDays(1)
        show()
        assertFalse(manager.isConfirmationDialogVisible)
    }

    @Test
    fun `no update, nothing offered`() {
        manager.setUpdateNotAvailable()
        show()
        assertFalse(manager.isConfirmationDialogVisible)
        compose.onNodeWithText("Restart").assertDoesNotExist()
    }
}
