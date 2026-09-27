package com.matheus.callmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.matheus.callmanager.core.database.entity.AllowListEntry
import kotlinx.coroutines.flow.Flow

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
