package com.matheus.callmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.matheus.callmanager.core.database.entity.AllowedNamePattern
import kotlinx.coroutines.flow.Flow

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
