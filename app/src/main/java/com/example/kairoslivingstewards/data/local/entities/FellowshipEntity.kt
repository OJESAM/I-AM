package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fellowships")
data class FellowshipEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val description: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val inviteCode: String = "",
    val leaderId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "fellowship_members")
data class FellowshipMemberEntity(
    @PrimaryKey val id: String = "",
    val fellowshipId: String = "",
    val userId: String = "",
    val role: String = "", // MEMBER, LEADER
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "fellowship_posts")
data class FellowshipPostEntity(
    @PrimaryKey val id: String = "",
    val fellowshipId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null, // IMAGE, VIDEO
    val timestamp: Long = System.currentTimeMillis()
)
