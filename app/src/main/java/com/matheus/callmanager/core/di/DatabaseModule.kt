package com.matheus.callmanager.core.di

import android.content.Context
import androidx.room.Room
import com.matheus.callmanager.core.database.CallManagerDatabase
import com.matheus.callmanager.core.database.migration.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideCallManagerDatabase(
        @ApplicationContext context: Context,
    ): CallManagerDatabase =
        Room
            .databaseBuilder(context, CallManagerDatabase::class.java, "call-manager.db")
            .addMigrations(MIGRATION_2_3)
            // Safety net for any version gap not covered by an explicit Migration above —
            // real Migrations should cover every version bump going forward so user data
            // (block list, allow list, etc.) survives app updates.
            .fallbackToDestructiveMigration()
            .build()
}
