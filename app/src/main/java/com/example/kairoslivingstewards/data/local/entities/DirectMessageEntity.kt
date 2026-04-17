package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @DocumentId @PrimaryKey val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isDelivered: Boolean = false
)
