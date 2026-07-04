package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicPlaybackService : Service() {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var repository: MusicRepository
    private var resolveJob: Job? = null
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackPosition = MutableStateFlow(0)
    val playbackPosition: StateFlow<Int> = _playbackPosition

    private val _playbackDuration = MutableStateFlow(0)
    val playbackDuration: StateFlow<Int> = _playbackDuration

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var progressJob: Job? = null

    // Playlist/Queue Management
    private var playlist: List<Song> = emptyList()
    private var currentSongIndex = -1

    companion object {
        private const val CHANNEL_ID = "yt_music_playback_channel"
        private const val NOTIFICATION_ID = 1122

        const val ACTION_PLAY = "com.example.ytmusic.PLAY"
        const val ACTION_PAUSE = "com.example.ytmusic.PAUSE"
        const val ACTION_NEXT = "com.example.ytmusic.NEXT"
        const val ACTION_PREV = "com.example.ytmusic.PREV"
        const val ACTION_STOP = "com.example.ytmusic.STOP"
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicPlaybackService", "Service Created")
        val database = AppDatabase.getDatabase(applicationContext)
        repository = MusicRepository(applicationContext, database.songDao())
        createNotificationChannel()
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { mp ->
                Log.d("MusicPlaybackService", "MediaPlayer Prepared")
                mp.start()
                _isPlaying.value = true
                _playbackDuration.value = mp.duration
                startProgressTracker()
                updateNotification()
            }
            setOnCompletionListener {
                Log.d("MusicPlaybackService", "Song Completed")
                playNext()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("MusicPlaybackService", "MediaPlayer error: what=$what, extra=$extra")
                _isPlaying.value = false
                false
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            Log.d("MusicPlaybackService", "Action Received: $action")
            when (action) {
                ACTION_PLAY -> resume()
                ACTION_PAUSE -> pause()
                ACTION_NEXT -> playNext()
                ACTION_PREV -> playPrevious()
                ACTION_STOP -> stopAndRemove()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setQueue(songs: List<Song>, startIndex: Int) {
        playlist = songs
        currentSongIndex = startIndex
        if (startIndex in playlist.indices) {
            playSong(playlist[startIndex])
        }
    }

    fun playSong(song: Song) {
        resolveJob?.cancel() // Cancel previous stream resolution if any
        _currentSong.value = song
        _playbackPosition.value = 0
        _playbackDuration.value = song.durationSeconds * 1000

        try {
            mediaPlayer?.reset()
        } catch (e: Exception) {
            Log.e("MusicPlaybackService", "Error resetting MediaPlayer", e)
        }

        // If downloaded offline, play immediately from local storage
        if (song.isDownloaded && !song.localFilePath.isNullOrEmpty()) {
            try {
                Log.d("MusicPlaybackService", "Playing offline downloaded file: ${song.localFilePath}")
                mediaPlayer?.let { setMediaPlayerDataSource(it, song.localFilePath) }
                mediaPlayer?.prepareAsync()
                _isPlaying.value = true
                startForeground(NOTIFICATION_ID, createNotification(song, true))
            } catch (e: Exception) {
                Log.e("MusicPlaybackService", "Error preparing offline song", e)
            }
            return
        }

        // Otherwise, show the playing/buffering notification and resolve the stream URL asynchronously
        startForeground(NOTIFICATION_ID, createNotification(song, true))

        resolveJob = serviceScope.launch {
            try {
                Log.d("MusicPlaybackService", "Resolving stream URL for song: ${song.title} (${song.id})")
                val resolvedUrl = repository.resolveStreamUrl(song)
                Log.d("MusicPlaybackService", "Resolved stream URL: $resolvedUrl")
                
                mediaPlayer?.reset()
                mediaPlayer?.let { setMediaPlayerDataSource(it, resolvedUrl) }
                mediaPlayer?.prepareAsync()
                _isPlaying.value = true
            } catch (e: Exception) {
                Log.e("MusicPlaybackService", "Error resolving and preparing online stream", e)
            }
        }
    }

    fun resume() {
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
            updateNotification()
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopProgressTracker()
            updateNotification()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _playbackPosition.value = positionMs
    }

    fun playNext() {
        if (playlist.isNotEmpty() && currentSongIndex < playlist.size - 1) {
            currentSongIndex++
            playSong(playlist[currentSongIndex])
        } else if (playlist.isNotEmpty()) {
            // Loop back to start
            currentSongIndex = 0
            playSong(playlist[0])
        }
    }

    fun playPrevious() {
        if (playlist.isNotEmpty() && currentSongIndex > 0) {
            currentSongIndex--
            playSong(playlist[currentSongIndex])
        } else if (playlist.isNotEmpty()) {
            // Loop back to end
            currentSongIndex = playlist.size - 1
            playSong(playlist[currentSongIndex])
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (true) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playbackPosition.value = it.currentPosition
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopAndRemove() {
        stopProgressTracker()
        mediaPlayer?.stop()
        _isPlaying.value = false
        _currentSong.value = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YT Music Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and status for music playback"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(song: Song, isPlaying: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Actions
        val prevPendingIntent = PendingIntent.getService(
            this, 1, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseActionIntent = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPausePendingIntent = PendingIntent.getService(
            this, 2, Intent(this, MusicPlaybackService::class.java).apply { action = playPauseActionIntent },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this, 3, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this, 4, Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopPendingIntent)
            )
            .setColor(0xFFFF0000.toInt()) // Red brand color for YouTube Music
            .setOngoing(isPlaying)
            .build()
    }

    private fun updateNotification() {
        val song = _currentSong.value ?: return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(song, _isPlaying.value))
    }

    private fun setMediaPlayerDataSource(mp: MediaPlayer, source: String) {
        val context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            createAttributionContext("media")
        } else {
            this
        }
        val uri = android.net.Uri.parse(source)
        try {
            if (source.startsWith("/") || source.startsWith("file://")) {
                val file = java.io.File(source)
                if (file.exists()) {
                    mp.setDataSource(context, android.net.Uri.fromFile(file))
                    return
                }
            }
            mp.setDataSource(context, uri)
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Uri setDataSource failed, falling back to String-based", e)
            try {
                mp.setDataSource(source)
            } catch (ex: Exception) {
                Log.e("MusicPlaybackService", "Fallback setDataSource also failed", ex)
            }
        }
    }

    override fun getAttributionTag(): String? {
        return "media"
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MusicPlaybackService", "Service Destroyed")
        serviceJob.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
