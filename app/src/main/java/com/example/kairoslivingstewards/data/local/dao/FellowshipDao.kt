package com.example.kairoslivingstewards.data.local.dao

import androidx.room.*
import com.example.kairoslivingstewards.data.local.entities.FellowshipEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipMemberEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FellowshipDao {
    @Query("SELECT * FROM fellowships")
    fun getAllFellowships(): Flow<List<FellowshipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFellowship(fellowship: FellowshipEntity)

    @Query("SELECT * FROM fellowships WHERE id = :fellowshipId")
    fun getFellowshipById(fellowshipId: String): Flow<FellowshipEntity?>

    @Query("SELECT * FROM fellowships WHERE inviteCode = :inviteCode")
    suspend fun getFellowshipByInviteCode(inviteCode: String): FellowshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun joinFellowship(member: FellowshipMemberEntity)

    @Query("SELECT * FROM fellowship_members WHERE userId = :userId")
    fun getJoinedFellowships(userId: String): Flow<List<FellowshipMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: FellowshipPostEntity)

    @Query("SELECT * FROM fellowship_posts WHERE fellowshipId = :fellowshipId ORDER BY timestamp DESC")
    fun getPostsByFellowship(fellowshipId: String): Flow<List<FellowshipPostEntity>>

    @Delete
    suspend fun deletePost(post: FellowshipPostEntity)

    @Query("DELETE FROM fellowship_members WHERE userId = :userId AND fellowshipId = :fellowshipId")
    suspend fun leaveFellowship(userId: String, fellowshipId: String)
}
