package com.example.kairoslivingstewards.data.local.dao

import androidx.room.*
import com.example.kairoslivingstewards.data.local.entities.DirectMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectMessageDao {
    @Query("SELECT * FROM direct_messages WHERE (senderId = :userId AND receiverId = :otherUserId) OR (senderId = :otherUserId AND receiverId = :userId) ORDER BY timestamp ASC")
    fun getMessagesBetweenUsers(userId: String, otherUserId: String): Flow<List<DirectMessageEntity>>

    @Query("SELECT * FROM direct_messages WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp DESC")
    fun getAllUserMessages(userId: String): Flow<List<DirectMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DirectMessageEntity)

    @Query("UPDATE direct_messages SET isRead = 1 WHERE receiverId = :userId AND senderId = :otherUserId")
    suspend fun markMessagesAsRead(userId: String, otherUserId: String)
}
