package com.matheus.callmanager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the `allowed_name_patterns` table; nothing to carry over from v2, no data touched. */
internal val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `allowed_name_patterns` " +
                    "(`pattern` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`pattern`))",
            )
        }
    }
