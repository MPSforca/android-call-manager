package com.matheus.callmanager.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A record of every call the screening service rejected, so it stays visible in the UI. */
@Entity(tableName = "blocked_call_log")
data class BlockedCallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val callerName: String?,
    val timestamp: Long,
    val reason: String,
)
