package com.example.kairoslivingstewards.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "livestream_settings")
data class LivestreamSettingsEntity(
    @PrimaryKey val id: Int = 1, // Singleton settings
    val youtubeVideoId: String = "",
    val commentsEnabled: Boolean = true
)
