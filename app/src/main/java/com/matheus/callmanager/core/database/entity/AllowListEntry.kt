package com.matheus.callmanager.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A number the user has explicitly allowed, regardless of other rules (e.g. picked from contacts). */
@Entity(tableName = "allow_list")
data class AllowListEntry(
    @PrimaryKey val number: String,
    val displayName: String?,
    val addedAt: Long,
)
