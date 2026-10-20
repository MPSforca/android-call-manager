package com.sforca.callmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sforca.callmanager.core.database.entity.BlockedCallLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallLogDao {
    @Query("SELECT * FROM blocked_call_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BlockedCallLog>>

    @Insert
    suspend fun insert(entry: BlockedCallLog)
}
