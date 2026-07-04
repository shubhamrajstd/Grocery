package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String, // YouTube Video/Track ID
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val coverUrl: String,
    val audioUrl: String, // Stream or cache URL
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null, // Set when downloaded offline
    val isLiked: Boolean = false,
    val lyrics: String? = null,
    val genre: String = "Unknown",
    val timestampAdded: Long = System.currentTimeMillis()
)
