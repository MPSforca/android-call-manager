package com.matheus.callmanager.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.matheus.callmanager.core.database.dao.AcceptedCallerDao
import com.matheus.callmanager.core.database.dao.AllowListDao
import com.matheus.callmanager.core.database.dao.AllowedNamePatternDao
import com.matheus.callmanager.core.database.dao.BlockListDao
import com.matheus.callmanager.core.database.dao.BlockedCallLogDao
import com.matheus.callmanager.core.database.entity.AcceptedCaller
import com.matheus.callmanager.core.database.entity.AllowListEntry
import com.matheus.callmanager.core.database.entity.AllowedNamePattern
import com.matheus.callmanager.core.database.entity.BlockListEntry
import com.matheus.callmanager.core.database.entity.BlockedCallLog
import com.matheus.callmanager.core.database.migration.MIGRATION_2_3

@Database(
    entities = [
        BlockListEntry::class,
        AllowListEntry::class,
        AcceptedCaller::class,
        BlockedCallLog::class,
        AllowedNamePattern::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class CallManagerDatabase : RoomDatabase() {
    abstract fun blockListDao(): BlockListDao

    abstract fun allowListDao(): AllowListDao

    abstract fun acceptedCallerDao(): AcceptedCallerDao

    abstract fun blockedCallLogDao(): BlockedCallLogDao

    abstract fun allowedNamePatternDao(): AllowedNamePatternDao

    companion object {
        @Volatile private var instance: CallManagerDatabase? = null

        fun get(context: Context): CallManagerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        CallManagerDatabase::class.java,
                        "call-manager.db",
                    ).addMigrations(MIGRATION_2_3)
                    // Safety net for any version gap not covered by an explicit Migration above —
                    // real Migrations should cover every version bump going forward so user data
                    // (block list, allow list, etc.) survives app updates.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
