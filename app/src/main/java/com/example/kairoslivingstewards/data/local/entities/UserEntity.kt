package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "",
    val username: String = "",
    val contact: String = "", // Email or Phone
    val profileImageUrl: String? = null,
    @get:PropertyName("isVerified") @set:PropertyName("isVerified")
    var isVerified: Boolean = false,
    val role: String = "USER", // USER, ADMIN, LEADER
    @get:PropertyName("isOnline") @set:PropertyName("isOnline")
    var isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val typingTo: String? = null,
    val fcmToken: String? = null
)
