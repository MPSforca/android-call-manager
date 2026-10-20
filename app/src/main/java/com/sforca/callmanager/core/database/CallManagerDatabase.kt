package com.sforca.callmanager.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sforca.callmanager.core.database.dao.AcceptedCallerDao
import com.sforca.callmanager.core.database.dao.AllowListDao
import com.sforca.callmanager.core.database.dao.AllowedNamePatternDao
import com.sforca.callmanager.core.database.dao.BlockListDao
import com.sforca.callmanager.core.database.dao.BlockedCallLogDao
import com.sforca.callmanager.core.database.entity.AcceptedCaller
import com.sforca.callmanager.core.database.entity.AllowListEntry
import com.sforca.callmanager.core.database.entity.AllowedNamePattern
import com.sforca.callmanager.core.database.entity.BlockListEntry
import com.sforca.callmanager.core.database.entity.BlockedCallLog

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
}
