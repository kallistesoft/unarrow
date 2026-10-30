package ca.kallistesoft.unarrow.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BuildTypeModule {
    /**
     * Debug: its own minimum version parameter, so the force-update flow can be tried without affecting users, and
     * Remote Config fetched on every refresh so console changes show at once.
     */
    @Provides
    fun buildTypeConfig() = BuildTypeConfig(
        minVersionParameter = "min_version_code_debug",
        featureFlagFetchIntervalSeconds = 0,
    )
}
