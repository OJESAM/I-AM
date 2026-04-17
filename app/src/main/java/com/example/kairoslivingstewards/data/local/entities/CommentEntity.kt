package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "comments")
data class CommentEntity(
    @DocumentId @PrimaryKey(autoGenerate = false) val id: String = "",
    val targetId: String = "", // ID of the devotional or "livestream"
    val userId: String = "",
    val userName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
