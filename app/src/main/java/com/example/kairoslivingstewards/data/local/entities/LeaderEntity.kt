package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaders")
data class LeaderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val meetingDay: String,
    val meetingTime: String,
    val imageUrl: String,
    val fellowshipCellName: String
)
