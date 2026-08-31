package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_items")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val songTitle: String,
    val artist: String = "",
    val youtubeVideoId: String = "",
    val youtubeUrl: String = "",
    val videoTitle: String = "",
    val channelName: String = "",
    val duration: String = "",
    val thumbnailUrl: String = "",
    val matchConfidence: Int = 90,
    val alternativeMatchesJson: String = "", // JSON list of alternative SearchResultItem
    val status: DownloadStatus = DownloadStatus.STAGING,
    val progress: Float = 0f,
    val downloadSpeed: String = "",
    val eta: String = "",
    val targetFilePath: String? = null,
    val mediaStoreUri: String? = null,
    val fileSizeBytes: Long = 0L,
    val audioFormat: String = "mp3",
    val audioQuality: String = "320k",
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
