package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetId: String, // ID of the devotional or "livestream"
    val userName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
