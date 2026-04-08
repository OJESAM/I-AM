package com.example.kairoslivingstewards.data.local.dao

import androidx.room.*
import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DevotionalDao {
    @Query("SELECT * FROM devotionals ORDER BY timestamp DESC")
    fun getAllDevotionals(): Flow<List<DevotionalEntity>>

    @Query("SELECT * FROM devotionals WHERE category = :category ORDER BY timestamp DESC")
    fun getDevotionalsByCategory(category: String): Flow<List<DevotionalEntity>>

    @Query("SELECT * FROM devotionals WHERE id = :id")
    suspend fun getDevotionalById(id: String): DevotionalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevotional(devotional: DevotionalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevotionals(devotionals: List<DevotionalEntity>)

    @Update
    suspend fun updateDevotional(devotional: DevotionalEntity)

    @Delete
    suspend fun deleteDevotional(devotional: DevotionalEntity)
}
