package com.matheus.callmanager.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.matheus.callmanager.core.datastore.DataStoreSettingsRepository
import com.matheus.callmanager.core.datastore.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            migrations = listOf(SharedPreferencesMigration(context, DataStoreSettingsRepository.LEGACY_PREFS_NAME)),
            produceFile = { context.preferencesDataStoreFile("settings") },
        )
}

@Module
@InstallIn(SingletonComponent::class)
interface SettingsBindingModule {
    @Binds
    fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
