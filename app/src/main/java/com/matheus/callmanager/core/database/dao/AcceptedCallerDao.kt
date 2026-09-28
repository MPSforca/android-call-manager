package com.matheus.callmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.matheus.callmanager.core.database.entity.AcceptedCaller

@Dao
interface AcceptedCallerDao {
    @Query("SELECT * FROM accepted_callers WHERE number = :number LIMIT 1")
    suspend fun get(number: String): AcceptedCaller?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(caller: AcceptedCaller)

    @Query("DELETE FROM accepted_callers WHERE lastAcceptedAt < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)
}
