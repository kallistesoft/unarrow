package ca.kallistesoft.unarrow.core.engagement

import android.app.Activity
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.data.Today
import ca.kallistesoft.unarrow.core.flags.FeatureFlag
import ca.kallistesoft.unarrow.core.flags.FeatureFlags
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import kotlinx.coroutines.flow.first

/**
 * Asks for Play's rating card after a win when [reviewDue] says so and `review_prompt` is on. Play decides whether
 * the card really shows (it has its own quota); either way the prompt counts as asked, so the game never asks again
 * within [REVIEW_SPACING_DAYS].
 */
internal class PlayReviewPrompt(
    private val manager: ReviewManager,
    private val progress: ProgressRepository,
    private val flags: FeatureFlags,
    private val today: Today,
    private val currentActivity: () -> Activity?,
) : ReviewPrompt {
    override suspend fun afterWin() {
        val p = progress.progress.first()
        if (!reviewDue(p.wins, p.lastReviewPromptDay, today())) return
        if (!flags.isEnabled(FeatureFlag.REVIEW_PROMPT).first()) return
        val activity = currentActivity() ?: return
        progress.markReviewPrompted()
        runCatching {
            val info = manager.requestReview()
            manager.launchReview(activity, info)
        } // Not installed from Play, or Play unavailable: no card, nothing to tell the player.
    }
}
