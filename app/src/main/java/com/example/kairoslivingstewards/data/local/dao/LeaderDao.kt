package com.example.kairoslivingstewards.data.local.dao

import androidx.room.*
import com.example.kairoslivingstewards.data.local.entities.LeaderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderDao {
    @Query("SELECT * FROM leaders")
    fun getAllLeaders(): Flow<List<LeaderEntity>>

    @Query("SELECT * FROM leaders WHERE id = :id")
    suspend fun getLeaderById(id: String): LeaderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeader(leader: LeaderEntity)

    @Update
    suspend fun updateLeader(leader: LeaderEntity)

    @Delete
    suspend fun deleteLeader(leader: LeaderEntity)
}
