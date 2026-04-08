package com.example.kairoslivingstewards.data.model

data class FellowshipCell(
    val id: String,
    val name: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val leader: Leader
)

data class Leader(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val meetingDay: String,
    val meetingTime: String,
    val imageUrl: String
)
