package ca.kallistesoft.unarrow.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import ca.kallistesoft.unarrow.core.data.DataStoreProgressRepository
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.data.GameProgressSerializer
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.data.Today
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun progress(impl: DataStoreProgressRepository): ProgressRepository

    companion object {
        @Provides
        @Singleton
        fun progressStore(@ApplicationContext context: Context): DataStore<GameProgress> = DataStoreFactory.create(
            serializer = GameProgressSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { GameProgressSerializer.defaultValue },
            produceFile = { context.dataStoreFile("game_progress.json") },
        )

        @Provides
        fun today(): Today = Today.System
    }
}
