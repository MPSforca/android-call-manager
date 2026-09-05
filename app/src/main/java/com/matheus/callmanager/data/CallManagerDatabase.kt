package com.matheus.callmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BlockListEntry::class, AllowListEntry::class, AcceptedCaller::class, BlockedCallLog::class],
    version = 2,
    exportSchema = false
)
abstract class CallManagerDatabase : RoomDatabase() {
    abstract fun blockListDao(): BlockListDao
    abstract fun allowListDao(): AllowListDao
    abstract fun acceptedCallerDao(): AcceptedCallerDao
    abstract fun blockedCallLogDao(): BlockedCallLogDao

    companion object {
        @Volatile private var instance: CallManagerDatabase? = null

        fun get(context: Context): CallManagerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CallManagerDatabase::class.java,
                    "call-manager.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
