package ca.kallistesoft.unarrow.core.engagement.di

import android.content.Context
import ca.kallistesoft.unarrow.core.common.CurrentActivity
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.data.Today
import ca.kallistesoft.unarrow.core.engagement.PlayReviewPrompt
import ca.kallistesoft.unarrow.core.engagement.ReviewPrompt
import ca.kallistesoft.unarrow.core.engagement.ShareSheet
import ca.kallistesoft.unarrow.core.engagement.Sharer
import ca.kallistesoft.unarrow.core.flags.FeatureFlags
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object EngagementModule {
    @Provides
    fun reviewPrompt(
        @ApplicationContext context: Context,
        progress: ProgressRepository,
        flags: FeatureFlags,
        today: Today,
        currentActivity: CurrentActivity,
    ): ReviewPrompt = PlayReviewPrompt(ReviewManagerFactory.create(context), progress, flags, today, currentActivity::get)

    @Provides
    fun sharer(@ApplicationContext context: Context, currentActivity: CurrentActivity): Sharer =
        ShareSheet(context, currentActivity::get)
}
