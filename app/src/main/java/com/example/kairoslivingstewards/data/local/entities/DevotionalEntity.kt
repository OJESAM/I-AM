package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devotionals")
data class DevotionalEntity(
    @PrimaryKey val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val date: String = "",
    val content: String = "",
    val scripture: String = "",
    val category: String = "Faith", // Faith, Prayer, Fasting, etc.
    val imageUrl: String? = null,
    val likesCount: Int = 0,
    val commentsEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
