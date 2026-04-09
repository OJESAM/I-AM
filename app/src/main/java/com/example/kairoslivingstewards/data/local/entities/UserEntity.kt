package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "",
    val username: String = "",
    val contact: String = "", // Email or Phone
    val profileImageUrl: String? = null,
    val isVerified: Boolean = false,
    val role: String = "USER" // USER, ADMIN, LEADER
)
