package ca.kallistesoft.unarrow.core.engagement

import android.app.Activity
import android.app.Application
import android.content.Intent
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.flags.FeatureFlag
import ca.kallistesoft.unarrow.core.testing.FakeFeatureFlags
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK])
class EngagementTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val today = 20_900L

    @Test
    fun `the rating card waits for three wins, then a month between asks`() {
        assertFalse(reviewDue(wins = 2, lastPromptDay = null, today = today))
        assertTrue(reviewDue(wins = 3, lastPromptDay = null, today = today))
        assertFalse(reviewDue(wins = 30, lastPromptDay = today - 29, today = today))
        assertTrue(reviewDue(wins = 30, lastPromptDay = today - 30, today = today))
    }

    @Test
    fun `a due rating card is asked once and remembered`() = runTest {
        val progress = FakeProgressRepository(GameProgress(wins = 3), today = today)
        prompt(progress).afterWin()
        assertEquals(today, progress.state.value.lastReviewPromptDay)
    }

    @Test
    fun `no rating card when not due, switched off remotely, or without a screen`() = runTest {
        val early = FakeProgressRepository(GameProgress(wins = 1), today = today)
        prompt(early).afterWin()
        assertNull(early.state.value.lastReviewPromptDay)

        val off = FakeProgressRepository(GameProgress(wins = 5), today = today)
        prompt(off, FakeFeatureFlags(FeatureFlag.REVIEW_PROMPT)).afterWin()
        assertNull(off.state.value.lastReviewPromptDay)

        val noScreen = FakeProgressRepository(GameProgress(wins = 5), today = today)
        prompt(noScreen, onScreen = null).afterWin()
        assertNull(noScreen.state.value.lastReviewPromptDay)
    }

    @Test
    fun `sharing opens the share sheet with the text`() {
        ShareSheet(app) { activity }.share("Solved! $PLAY_STORE_URL", "Share")
        val chooser = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("Solved! $PLAY_STORE_URL", send.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("text/plain", send.type)
    }

    @Test
    fun `sharing without a screen starts a new task`() {
        ShareSheet(app) { null }.share("text", "Share")
        val chooser = shadowOf(app).nextStartedActivity
        assertTrue(chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    private fun prompt(
        progress: FakeProgressRepository,
        flags: FakeFeatureFlags = FakeFeatureFlags(),
        onScreen: Activity? = activity,
    ) = PlayReviewPrompt(FakeReviewManager(app), progress, flags, { today }, { onScreen })
}
