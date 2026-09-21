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

    @Query("SELECT * FROM block_list WHERE number = :number LIMIT 1")
    suspend fun getIfExists(number: String): BlockListEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<BlockListEntry>)

    @Delete
    suspend fun delete(entry: BlockListEntry)
}

@Dao
interface AllowListDao {
    @Query("SELECT * FROM allow_list ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<AllowListEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM allow_list WHERE number = :number)")
    suspend fun isAllowed(number: String): Boolean

    @Query("SELECT * FROM allow_list WHERE number = :number LIMIT 1")
    suspend fun getIfExists(number: String): AllowListEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<AllowListEntry>)

    @Delete
    suspend fun delete(entry: AllowListEntry)
}

@Dao
interface AllowedNamePatternDao {
    @Query("SELECT * FROM allowed_name_patterns ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<AllowedNamePattern>>

    @Query("SELECT pattern FROM allowed_name_patterns")
    suspend fun getAllPatterns(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AllowedNamePattern)

    @Delete
    suspend fun delete(entry: AllowedNamePattern)
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
