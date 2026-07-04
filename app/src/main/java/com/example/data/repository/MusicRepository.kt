package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.SongDao
import com.example.data.model.PlaybackHistory
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Database Queries
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val downloadedSongs: Flow<List<Song>> = songDao.getDownloadedSongs()
    val likedSongs: Flow<List<Song>> = songDao.getLikedSongs()
    val recentSongs: Flow<List<Song>> = songDao.getRecentSongs()

    // Pre-curated high-quality audio streams (Royalty-free)
    val defaultLibrary = listOf(
        Song(
            id = "yt_synth_01",
            title = "Midnight Horizon",
            artist = "Neon Horizon",
            album = "Cyberpunk Boulevard",
            durationSeconds = 312,
            coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            genre = "Synthwave",
            lyrics = "[00:00] (Instrumental Neon Intro)\n[00:15] Driving into the midnight sun...\n[00:30] Neon lights are shining bright...\n[00:45] Can you feel the horizon calling us tonight?\n[01:05] Shadows dance in the cyber lane...\n[01:25] (Guitar Synthesizer Solo)\n[02:00] We are the riders of the cyber light.\n[02:40] Out of time, into the cyber skyline..."
        ),
        Song(
            id = "yt_lofi_02",
            title = "Chilled Coffee",
            artist = "Lofi Study Club",
            album = "Warm Mornings",
            durationSeconds = 425,
            coverUrl = "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=500&auto=format&fit=crop&q=60",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            genre = "Lofi Chill",
            lyrics = "[00:00] (Vinyl crackle and coffee brewing sounds)\n[00:20] Relax, take a deep breath...\n[00:45] Sun rays through the dusty pane...\n[01:10] Warm espresso washes the stress away.\n[01:45] (Smooth Trumpet & Jazz Piano Interlude)\n[02:30] Let the beat calm your mind...\n[03:15] Chill lofi beats for you to unwind."
        ),
        Song(
            id = "yt_pop_03",
            title = "Golden Dream",
            artist = "Aura Keys",
            album = "Ethereal Echoes",
            durationSeconds = 302,
            coverUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=60",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            genre = "Pop",
            lyrics = "[00:00] (Acoustic Guitar Intro)\n[00:12] Golden sand under my feet...\n[00:28] Remembering the summer heat...\n[00:44] We were dreaming, flying so high...\n[01:00] Underneath the golden summer sky!\n[01:20] Keep on dreaming, never let it fade.\n[01:50] (Piano Bridge)\n[02:15] Let the golden dreams guide you home."
        ),
        Song(
            id = "yt_electro_04",
            title = "Grid Runner",
            artist = "Byte Knight",
            album = "Digital Kingdom",
            durationSeconds = 302,
            coverUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=500&auto=format&fit=crop&q=60",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            genre = "Electronic",
            lyrics = "[00:00] (Fast Electronic Beat Intro)\n[00:10] Executing sequence 109...\n[00:25] We are surfing on the data line!\n[00:40] Running on the digital grid...\n[01:00] Hack the matrix, download the mind.\n[01:30] (Techno Bass Drop)\n[02:10] Byte by byte, we conquer the code."
        ),
        Song(
            id = "yt_jazz_05",
            title = "Midnight Lounge",
            artist = "Vibe Station",
            album = "Espresso Jazz",
            durationSeconds = 363,
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=60",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            genre = "Jazz",
            lyrics = "[00:00] (Soft Saxophone Intro)\n[00:30] Rainy nights, neon streets...\n[01:00] Jazz notes playing slow beats...\n[01:30] Sipping coffee, watching the rain...\n[02:00] Smooth melodies heal the pain.\n[02:40] (Deep Double Bass and Piano Solos)\n[03:20] Close your eyes and feel the vibe."
        )
    )

    suspend fun insertSong(song: Song) = withContext(Dispatchers.IO) {
        songDao.insertSong(song)
    }

    suspend fun toggleLike(song: Song) = withContext(Dispatchers.IO) {
        val updated = song.copy(isLiked = !song.isLiked)
        songDao.insertSong(updated)
    }

    suspend fun insertHistory(songId: String) = withContext(Dispatchers.IO) {
        songDao.insertHistory(PlaybackHistory(songId = songId))
    }

    // Scrape real YouTube search results
    suspend fun scrapeYouTubeSearch(query: String): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIQAQ%253D%253D"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("MusicRepository", "YouTube search HTTP error: ${response.code}")
                    return@withContext emptyList()
                }
                
                val html = response.body?.string() ?: return@withContext emptyList()
                
                val regex = Regex("""ytInitialData\s*=\s*(\{.+?\});""", RegexOption.DOT_MATCHES_ALL)
                val match = regex.find(html)
                val jsonStr = match?.groupValues?.get(1)
                
                if (jsonStr != null) {
                    val json = JSONObject(jsonStr)
                    val contentsArray = json.optJSONObject("contents")
                        ?.optJSONObject("twoColumnSearchResultsRenderer")
                        ?.optJSONObject("primaryContents")
                        ?.optJSONObject("sectionListRenderer")
                        ?.optJSONArray("contents")

                    if (contentsArray != null) {
                        for (i in 0 until contentsArray.length()) {
                            val sectionObj = contentsArray.optJSONObject(i)
                            val itemSection = sectionObj?.optJSONObject("itemSectionRenderer")
                            val itemsArray = itemSection?.optJSONArray("contents") ?: continue
                            for (j in 0 until itemsArray.length()) {
                                val itemObj = itemsArray.optJSONObject(j)
                                val videoRenderer = itemObj?.optJSONObject("videoRenderer") ?: continue
                                
                                val videoId = videoRenderer.optString("videoId") ?: continue
                                if (videoId.isEmpty()) continue
                                
                                val titleObj = videoRenderer.optJSONObject("title")
                                val title = titleObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                    ?: titleObj?.optString("accessibility")
                                    ?: "Unknown Title"
                                    
                                val ownerObj = videoRenderer.optJSONObject("ownerText") ?: videoRenderer.optJSONObject("shortBylineText")
                                val artist = ownerObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                    ?: "YouTube Creator"
                                    
                                val thumbnailObj = videoRenderer.optJSONObject("thumbnail")
                                val thumbnailsArray = thumbnailObj?.optJSONArray("thumbnails")
                                val coverUrl = thumbnailsArray?.optJSONObject(thumbnailsArray.length() - 1)?.optString("url")
                                    ?: thumbnailsArray?.optJSONObject(0)?.optString("url")
                                    ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60"
                                    
                                val lengthObj = videoRenderer.optJSONObject("lengthText")
                                val durationText = lengthObj?.optString("simpleText") ?: "0:00"
                                val durationSeconds = parseDurationToSeconds(durationText)
                                
                                val song = Song(
                                    id = videoId,
                                    title = title,
                                    artist = artist,
                                    album = "YouTube Single",
                                    durationSeconds = durationSeconds,
                                    coverUrl = coverUrl,
                                    audioUrl = "", // Resolved on-demand when playing
                                    genre = "YouTube",
                                    lyrics = "[00:00] Playing from YouTube stream...\n[00:15] Title: $title\n[00:30] Channel: $artist\n[00:45] Enjoy ad-free premium listening."
                                )
                                list.add(song)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to scrape YouTube search", e)
        }
        return@withContext list
    }

    private fun parseDurationToSeconds(durationText: String): Int {
        val parts = durationText.split(":")
        return try {
            when (parts.size) {
                1 -> parts[0].toIntOrNull() ?: 0
                2 -> {
                    val min = parts[0].toIntOrNull() ?: 0
                    val sec = parts[1].toIntOrNull() ?: 0
                    min * 60 + sec
                }
                3 -> {
                    val hr = parts[0].toIntOrNull() ?: 0
                    val min = parts[1].toIntOrNull() ?: 0
                    val sec = parts[2].toIntOrNull() ?: 0
                    hr * 3600 + min * 60 + sec
                }
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    // Resolves any video ID or song title to a direct, playable audio stream URL
    suspend fun resolveStreamUrl(song: Song): String = withContext(Dispatchers.IO) {
        var videoId = song.id
        
        // If it's a pre-curated or AI-curated song, search YouTube for its real audio stream first
        if (song.id.startsWith("yt_")) {
            val searchQuery = "${song.artist} - ${song.title}"
            val searchResults = scrapeYouTubeSearch(searchQuery)
            if (searchResults.isNotEmpty()) {
                videoId = searchResults[0].id
                Log.d("MusicRepository", "Resolved song '${song.title}' to real YouTube video ID: $videoId")
            }
        }
        
        // Piped API instances for fast streaming
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.colby.land",
            "https://pipedapi.us.to",
            "https://pipedapi.drg.sh",
            "https://pipedapi.astre.me"
        )
        
        for (instance in pipedInstances) {
            try {
                val url = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            val audioStreams = json.optJSONArray("audioStreams")
                            if (audioStreams != null && audioStreams.length() > 0) {
                                var bestStreamUrl = ""
                                var maxBitrate = -1
                                for (i in 0 until audioStreams.length()) {
                                    val stream = audioStreams.getJSONObject(i)
                                    val streamUrl = stream.optString("url") ?: continue
                                    val bitrate = stream.optInt("bitrate", -1)
                                    val mimeType = stream.optString("mimeType", "")
                                    if (mimeType.contains("audio")) {
                                        if (bitrate > maxBitrate) {
                                            maxBitrate = bitrate
                                            bestStreamUrl = streamUrl
                                        }
                                    }
                                }
                                if (bestStreamUrl.isEmpty()) {
                                    bestStreamUrl = audioStreams.getJSONObject(0).optString("url")
                                }
                                if (bestStreamUrl.isNotEmpty()) {
                                    Log.d("MusicRepository", "Resolved video $videoId to audio stream URL using $instance")
                                    return@withContext bestStreamUrl
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicRepository", "Piped instance $instance failed for $videoId", e)
            }
        }
        
        // Invidious API instances as backup
        val invidiousInstances = listOf(
            "https://yewtu.be",
            "https://vid.puffyan.us",
            "https://inv.tux.im",
            "https://invidious.snopyta.org"
        )
        for (instance in invidiousInstances) {
            try {
                val url = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                            if (adaptiveFormats != null) {
                                for (i in 0 until adaptiveFormats.length()) {
                                    val format = adaptiveFormats.getJSONObject(i)
                                    val type = format.optString("type", "")
                                    val streamUrl = format.optString("url")
                                    if (type.contains("audio") && streamUrl.isNotEmpty()) {
                                        Log.d("MusicRepository", "Resolved video $videoId to audio stream URL using Invidious $instance")
                                        return@withContext streamUrl
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicRepository", "Invidious instance $instance failed for $videoId", e)
            }
        }
        
        Log.e("MusicRepository", "Failed to resolve stream URL for $videoId. Falling back to default URL.")
        return@withContext song.audioUrl.ifEmpty { "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" }
    }

    // Searches the library & YouTube (scraped)
    fun searchSongs(query: String): Flow<List<Song>> = flow {
        val searchQuery = query.ifBlank { "Top Songs" }
        val scrapedResults = scrapeYouTubeSearch(searchQuery)
        if (scrapedResults.isNotEmpty()) {
            emit(scrapedResults)
        } else {
            val filtered = defaultLibrary.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.genre.contains(query, ignoreCase = true)
            }
            emit(filtered)
        }
    }.flowOn(Dispatchers.IO)

    // Download song to local storage for authentic Offline Playback
    suspend fun downloadSong(song: Song, onProgress: (Int) -> Unit): Result<Song> = withContext(Dispatchers.IO) {
        try {
            // Check if directory exists
            val downloadDir = File(context.filesDir, "downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val destinationFile = File(downloadDir, "${song.id}.mp3")
            if (destinationFile.exists()) {
                // Already downloaded, just update DB
                val updatedSong = song.copy(isDownloaded = true, localFilePath = destinationFile.absolutePath)
                songDao.insertSong(updatedSong)
                return@withContext Result.success(updatedSong)
            }

            // Resolve streaming URL first if empty or mock
            var audioUrl = song.audioUrl
            if (audioUrl.isEmpty() || !audioUrl.startsWith("http") || song.id.startsWith("yt_")) {
                Log.d("MusicRepository", "Resolving stream URL before downloading: ${song.title}")
                audioUrl = resolveStreamUrl(song)
            }

            Log.d("MusicRepository", "Downloading starting: $audioUrl")
            onProgress(10)

            // Make network call and stream file
            val url = URL(audioUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            val fileLength = connection.contentLength
            val input = connection.getInputStream()
            val output = FileOutputStream(destinationFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress(10 + (total * 80 / fileLength).toInt())
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            onProgress(95)

            // Update database
            val updatedSong = song.copy(
                isDownloaded = true,
                localFilePath = destinationFile.absolutePath
            )
            songDao.insertSong(updatedSong)
            onProgress(100)

            Result.success(updatedSong)
        } catch (e: Exception) {
            Log.e("MusicRepository", "Download failed for song ${song.id}", e)
            Result.failure(e)
        }
    }

    suspend fun removeDownload(song: Song) = withContext(Dispatchers.IO) {
        song.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        val updated = song.copy(isDownloaded = false, localFilePath = null)
        songDao.insertSong(updated)
    }

    // Curate personalized daily mixes using the Gemini API!
    suspend fun generateDailyMixes(favoriteGenres: List<String>): List<Song> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("MusicRepository", "Gemini API key is not configured. Falling back to local curation.")
            return@withContext getLocalCuratedMixes(favoriteGenres)
        }

        val genresStr = favoriteGenres.joinToString(", ")
        val prompt = """
            Generate 4 customized, premium song metadata entries for a personalized daily mix.
            The user's favorite genres are: $genresStr.
            
            Return ONLY a JSON array containing objects with these exact keys:
            - "title": A creative song title fitting the genres.
            - "artist": A cool, unique fictive band/musician name.
            - "album": A creative album title.
            - "genre": One of the requested genres.
            - "durationSeconds": A random integer between 180 and 360.
            - "lyrics": Brief poetic song lyrics (at least 4 lines with [mm:ss] timecodes at the start of each line).
            
            Do not include any other markdown formatting outside of a valid JSON array block.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        try {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("MusicRepository", "Gemini API error: ${response.code} ${response.message}")
                    return@withContext getLocalCuratedMixes(favoriteGenres)
                }

                val body = response.body?.string() ?: return@withContext getLocalCuratedMixes(favoriteGenres)
                val jsonResponse = JSONObject(body)
                val candidates = jsonResponse.optJSONArray("candidates")
                val textResponse = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                val jsonArray = JSONArray(textResponse.trim())
                val list = mutableListOf<Song>()
                val testStreams = listOf(
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"
                )
                val coverImages = listOf(
                    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60"
                )

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = "yt_gemini_mix_${System.currentTimeMillis()}_$i"
                    val song = Song(
                        id = id,
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.getString("album"),
                        durationSeconds = obj.getInt("durationSeconds"),
                        coverUrl = coverImages[i % coverImages.size],
                        audioUrl = testStreams[i % testStreams.size],
                        genre = obj.getString("genre"),
                        lyrics = obj.optString("lyrics", "No lyrics available.")
                    )
                    // Insert into DB so it's persistent
                    songDao.insertSong(song)
                    list.add(song)
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to generate dynamic daily mixes", e)
            return@withContext getLocalCuratedMixes(favoriteGenres)
        }
    }

    private fun getLocalCuratedMixes(favoriteGenres: List<String>): List<Song> {
        val list = mutableListOf<Song>()
        val defaultPool = defaultLibrary
        // Map based on user's preference or just use pool
        favoriteGenres.forEachIndexed { index, genre ->
            val match = defaultPool.find { it.genre.contains(genre, ignoreCase = true) }
            if (match != null) {
                list.add(match.copy(id = "yt_mix_local_${index}_${match.id}"))
            } else {
                // Synthesize from pool
                val fallback = defaultPool[index % defaultPool.size]
                list.add(fallback.copy(
                    id = "yt_mix_local_${index}_${fallback.id}",
                    title = "Daily $genre Breeze",
                    artist = "Vibe Masters",
                    genre = genre
                ))
            }
        }

        if (list.isEmpty()) {
            list.addAll(defaultPool)
        }
        return list
    }
}
