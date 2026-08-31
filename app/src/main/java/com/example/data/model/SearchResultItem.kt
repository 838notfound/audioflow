package com.example.data.model

data class SearchResultItem(
    val videoId: String,
    val title: String,
    val channel: String,
    val duration: String,
    val durationSeconds: Int = 0,
    val thumbnailUrl: String,
    val url: String,
    val viewCount: String = "",
    val confidence: Int = 90
)
