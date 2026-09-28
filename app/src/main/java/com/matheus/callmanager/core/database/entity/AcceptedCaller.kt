package com.matheus.callmanager.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A number that was recently allowed through (answered by the user).
 * Backs the "allow calls from accepted sources within the past X days" rule.
 */
@Entity(tableName = "accepted_callers")
data class AcceptedCaller(
    @PrimaryKey val number: String,
    val lastAcceptedAt: Long,
)
