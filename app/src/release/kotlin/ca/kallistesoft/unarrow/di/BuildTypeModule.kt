package ca.kallistesoft.unarrow.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BuildTypeModule {
    /** Release: the minimum version users get, and Remote Config fetched at most hourly (its quota). */
    @Provides
    fun buildTypeConfig() = BuildTypeConfig(
        minVersionParameter = "min_version_code",
        featureFlagFetchIntervalSeconds = 3_600,
    )
}
