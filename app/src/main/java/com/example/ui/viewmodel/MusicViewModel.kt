package com.example.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    // Service Connection
    private var serviceBinder: MusicPlaybackService.LocalBinder? = null
    private var boundService: MusicPlaybackService? = null
    
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound = _isServiceBound.asStateFlow()

    // Exposed Flows from Service (dynamically updated when bound)
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0)
    val playbackPosition: StateFlow<Int> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0)
    val playbackDuration: StateFlow<Int> = _playbackDuration.asStateFlow()

    // Database / Repository Flows
    val defaultLibrary = repository.defaultLibrary

    private val _quickPicks = MutableStateFlow<List<Song>>(emptyList())
    val quickPicks: StateFlow<List<Song>> = _quickPicks.asStateFlow()

    init {
        loadQuickPicks()
    }

    fun loadQuickPicks() {
        viewModelScope.launch {
            try {
                val songs = repository.scrapeYouTubeSearch("Trending Hits")
                if (songs.isNotEmpty()) {
                    _quickPicks.value = songs
                } else {
                    _quickPicks.value = repository.defaultLibrary
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to load quick picks from YouTube scraping", e)
                _quickPicks.value = repository.defaultLibrary
            }
        }
    }

    val likedSongs: StateFlow<List<Song>> = repository.likedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<Song>> = repository.downloadedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSongs: StateFlow<List<Song>> = repository.recentSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Query Flow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Song>> = _searchQuery
        .flatMapLatest { query ->
            repository.searchSongs(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultLibrary)

    // Personalized Daily Mixes Flow
    private val _dailyMixes = MutableStateFlow<List<Song>>(emptyList())
    val dailyMixes = _dailyMixes.asStateFlow()

    private val _isGeneratingMixes = MutableStateFlow(false)
    val isGeneratingMixes = _isGeneratingMixes.asStateFlow()

    // Favorite Genres for Mix Customization
    private val _favoriteGenres = MutableStateFlow(listOf("Lofi", "Synthwave", "Pop"))
    val favoriteGenres = _favoriteGenres.asStateFlow()

    // Downloads Progress Map (songId -> percentage progress)
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MusicViewModel", "Service Connected")
            val binder = service as MusicPlaybackService.LocalBinder
            serviceBinder = binder
            val s = binder.getService()
            boundService = s
            _isServiceBound.value = true

            // Observe playback state changes from the service
            viewModelScope.launch {
                s.currentSong.collect { _currentSong.value = it }
            }
            viewModelScope.launch {
                s.isPlaying.collect { _isPlaying.value = it }
            }
            viewModelScope.launch {
                s.playbackPosition.collect { _playbackPosition.value = it }
            }
            viewModelScope.launch {
                s.playbackDuration.collect { _playbackDuration.value = it }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("MusicViewModel", "Service Disconnected")
            serviceBinder = null
            boundService = null
            _isServiceBound.value = false
        }
    }

    fun bindPlaybackService(context: Context) {
        val intent = Intent(context, MusicPlaybackService::class.java)
        context.startService(intent) // Ensure it runs in foreground
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindPlaybackService(context: Context) {
        if (_isServiceBound.value) {
            context.unbindService(serviceConnection)
            _isServiceBound.value = false
        }
    }

    // Playback Operations
    fun playSongFromList(song: Song, queue: List<Song>) {
        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        
        // Save to Recent Playback History
        viewModelScope.launch {
            repository.insertSong(song)
            repository.insertHistory(song.id)
        }

        // Send to service
        boundService?.setQueue(queue, startIndex)
    }

    fun togglePlayPause() {
        val s = boundService ?: return
        if (s.isPlaying.value) {
            s.pause()
        } else {
            s.resume()
        }
    }

    fun skipNext() {
        boundService?.playNext()
    }

    fun skipPrevious() {
        boundService?.playPrevious()
    }

    fun seekTo(positionMs: Int) {
        boundService?.seekTo(positionMs)
    }

    // Song Meta Operations
    fun toggleLikeSong(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song)
            
            // If the song is currently playing, update the current song flow
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isLiked = !song.isLiked)
            }
        }
    }

    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    // Update favorite genres and generate mixes
    fun updateFavoriteGenres(genres: List<String>) {
        _favoriteGenres.value = genres
        generateDailyMixes()
    }

    fun generateDailyMixes() {
        viewModelScope.launch {
            _isGeneratingMixes.value = true
            try {
                val mixes = repository.generateDailyMixes(_favoriteGenres.value)
                _dailyMixes.value = mixes
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error creating mix", e)
            } finally {
                _isGeneratingMixes.value = false
            }
        }
    }

    // Download/Offline Listening Operations
    fun downloadSong(song: Song) {
        if (song.isDownloaded) return
        
        viewModelScope.launch {
            // Pre-save song to DB first if it's dynamic
            repository.insertSong(song)
            
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                put(song.id, 0)
            }
            
            val result = repository.downloadSong(song) { progress ->
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    put(song.id, progress)
                }
            }

            result.onSuccess { downloadedSong ->
                Log.d("MusicViewModel", "Song downloaded successfully: ${downloadedSong.title}")
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    remove(song.id)
                }
                
                // Update playing song if it was downloaded during playback
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = downloadedSong
                }
            }.onFailure {
                Log.e("MusicViewModel", "Song download failed", it)
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    remove(song.id)
                }
            }
        }
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            repository.removeDownload(song)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isDownloaded = false, localFilePath = null)
            }
        }
    }

    class Factory(private val repository: MusicRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                return MusicViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
