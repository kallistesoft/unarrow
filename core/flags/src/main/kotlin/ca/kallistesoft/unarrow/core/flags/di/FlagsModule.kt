package ca.kallistesoft.unarrow.core.flags.di

import android.content.Context
import ca.kallistesoft.unarrow.core.flags.DefaultFeatureFlags
import ca.kallistesoft.unarrow.core.flags.FeatureFlagSettings
import ca.kallistesoft.unarrow.core.flags.FeatureFlags
import ca.kallistesoft.unarrow.core.flags.FirebaseFeatureFlags
import ca.kallistesoft.unarrow.core.flags.NoRemoteConfigValues
import ca.kallistesoft.unarrow.core.flags.RemoteConfigValues
import com.google.firebase.FirebaseApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** Firebase Remote Config when the build has a google-services.json (FirebaseApp initialised), otherwise none. */
internal class RemoteConfigHolder(val firebase: FirebaseFeatureFlags?)

@Module
@InstallIn(SingletonComponent::class)
internal object FlagsModule {
    @Provides
    @Singleton
    fun remoteConfig(@ApplicationContext context: Context, settings: FeatureFlagSettings): RemoteConfigHolder =
        RemoteConfigHolder(
            if (FirebaseApp.getApps(context).isEmpty()) {
                null
            } else {
                FirebaseFeatureFlags.create(settings, CoroutineScope(SupervisorJob() + Dispatchers.Default))
            },
        )

    /** Remote flags with Firebase, defaults otherwise. */
    @Provides
    @Singleton
    fun featureFlags(holder: RemoteConfigHolder): FeatureFlags = holder.firebase ?: DefaultFeatureFlags()

    /** Remote values with Firebase; without it nothing is known remotely. */
    @Provides
    @Singleton
    fun remoteConfigValues(holder: RemoteConfigHolder): RemoteConfigValues = holder.firebase ?: NoRemoteConfigValues
}
