package com.matheus.callmanager.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A number the user has explicitly blocked (via batch-block, contact picker, or manual add). */
@Entity(tableName = "block_list")
data class BlockListEntry(
    @PrimaryKey val number: String,
    val displayName: String?,
    val addedAt: Long,
)
