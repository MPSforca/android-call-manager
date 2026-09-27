package com.matheus.callmanager.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A case-insensitive "contains" pattern matched against a caller's carrier-supplied caller ID name
 * (CNAM), e.g. "PagerDuty" — for callers whose caller ID name is consistent but whose number rotates.
 */
@Entity(tableName = "allowed_name_patterns")
data class AllowedNamePattern(
    @PrimaryKey val pattern: String,
    val addedAt: Long,
)
