package com.matheus.callmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockListDao {
    @Query("SELECT * FROM block_list ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<BlockListEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM block_list WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<BlockListEntry>)

    @Delete
    suspend fun delete(entry: BlockListEntry)
}

@Dao
interface AcceptedCallerDao {
    @Query("SELECT * FROM accepted_callers WHERE number = :number LIMIT 1")
    suspend fun get(number: String): AcceptedCaller?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(caller: AcceptedCaller)

    @Query("DELETE FROM accepted_callers WHERE lastAcceptedAt < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)
}

@Dao
interface BlockedCallLogDao {
    @Query("SELECT * FROM blocked_call_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BlockedCallLog>>

    @Insert
    suspend fun insert(entry: BlockedCallLog)
}
