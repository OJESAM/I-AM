package com.example.kairoslivingstewards.data.model

data class Devotional(
    val id: String,
    val title: String,
    val date: String,
    val content: String,
    val scripture: String,
    val imageUrl: String? = null,
    val likesCount: Int = 0,
    val commentsEnabled: Boolean = true
)
