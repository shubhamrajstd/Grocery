package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: String,
    val timestamp: Long = System.currentTimeMillis()
)
