package com.matheus.callmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A number the user has explicitly blocked (via batch-block or manual add). */
@Entity(tableName = "block_list")
data class BlockListEntry(
    @PrimaryKey val number: String,
    val addedAt: Long
)

/**
 * A number that was recently allowed through (answered by the user).
 * Backs the "allow calls from accepted sources within the past X days" rule.
 */
@Entity(tableName = "accepted_callers")
data class AcceptedCaller(
    @PrimaryKey val number: String,
    val lastAcceptedAt: Long
)

/** A record of every call the screening service rejected, so it stays visible in the UI. */
@Entity(tableName = "blocked_call_log")
data class BlockedCallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val callerName: String?,
    val timestamp: Long,
    val reason: String
)
