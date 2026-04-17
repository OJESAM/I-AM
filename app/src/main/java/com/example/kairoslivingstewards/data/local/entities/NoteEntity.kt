package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "notes")
data class NoteEntity(
    @DocumentId @PrimaryKey val id: String = "",
    val targetId: String = "", // e.g., livestream or a specific devotional id
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
