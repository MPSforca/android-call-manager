package com.sforca.callmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sforca.callmanager.core.database.entity.BlockListEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockListDao {
    @Query("SELECT * FROM block_list ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<BlockListEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM block_list WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean

    @Query("SELECT * FROM block_list WHERE number = :number LIMIT 1")
    suspend fun getIfExists(number: String): BlockListEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<BlockListEntry>)

    @Delete
    suspend fun delete(entry: BlockListEntry)
}
