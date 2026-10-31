package ca.kallistesoft.unarrow.core.common.di

import android.app.Application
import android.content.Context
import ca.kallistesoft.unarrow.core.common.CurrentActivity
import ca.kallistesoft.unarrow.core.common.ResumedActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CommonModule {
    /** Created when the activity is injected, before it resumes: the first resumed activity is seen. */
    @Provides
    @Singleton
    fun currentActivity(@ApplicationContext context: Context): CurrentActivity =
        ResumedActivity().also { (context.applicationContext as Application).registerActivityLifecycleCallbacks(it) }
}
