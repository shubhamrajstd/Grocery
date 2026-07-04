package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PlaybackHistory
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY timestampAdded DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongById(id: String): Flow<Song?>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongByIdDirect(id: String): Song?

    @Query("SELECT * FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isLiked = 1")
    fun getLikedSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    // History Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistory)

    @Query("""
        SELECT songs.* FROM songs 
        INNER JOIN playback_history ON songs.id = playback_history.songId 
        ORDER BY playback_history.timestamp DESC 
        LIMIT :limit
    """)
    fun getRecentSongs(limit: Int = 10): Flow<List<Song>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
