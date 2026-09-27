package com.matheus.callmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

        /** Adds the `allowed_name_patterns` table; nothing to carry over from v2, no data touched. */
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `allowed_name_patterns` " +
                            "(`pattern` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`pattern`))",
                    )
                }
            }

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
