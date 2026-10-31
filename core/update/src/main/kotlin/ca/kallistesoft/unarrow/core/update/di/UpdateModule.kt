package ca.kallistesoft.unarrow.core.update.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import ca.kallistesoft.unarrow.core.update.ForceUpdateStore
import ca.kallistesoft.unarrow.core.update.MinVersionSource
import ca.kallistesoft.unarrow.core.update.RemoteConfigMinVersionSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateModule {
    @Binds
    abstract fun minVersionSource(impl: RemoteConfigMinVersionSource): MinVersionSource

    companion object {
        @Provides
        @Singleton
        @ForceUpdateStore
        fun store(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("force_update") })
    }
}
