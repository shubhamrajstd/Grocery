package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardGrey
import com.example.ui.theme.TextSilver
import com.example.ui.theme.YTRed
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onSongSelected: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Downloads, 1: Liked
    val tabs = listOf("Offline Downloads", "Liked Songs")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Stats & AI Insight Card
            ListeningInsightsCard(
                downloadCount = downloadedSongs.size,
                likedCount = likedSongs.size,
                topGenre = if (likedSongs.isNotEmpty()) likedSongs.groupBy { it.genre }.maxByOrNull { it.value.size }?.key ?: "Lofi" else "Lofi"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color.White else TextSilver
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            when (selectedTab) {
                0 -> DownloadsTab(
                    songs = downloadedSongs,
                    downloadProgress = downloadProgress,
                    onPlay = { song -> onSongSelected(song, downloadedSongs) },
                    onDelete = { song -> viewModel.deleteDownload(song) }
                )
                1 -> LikedSongsTab(
                    songs = likedSongs,
                    onPlay = { song -> onSongSelected(song, likedSongs) },
                    onShuffle = {
                        if (likedSongs.isNotEmpty()) {
                            val shuffled = likedSongs.shuffled()
                            onSongSelected(shuffled.first(), shuffled)
                        }
                    },
                    onToggleLike = { song -> viewModel.toggleLikeSong(song) },
                    onDownload = { song -> viewModel.downloadSong(song) },
                    downloadProgress = downloadProgress
                )
            }
        }
    }
}

@Composable
fun ListeningInsightsCard(
    downloadCount: Int,
    likedCount: Int,
    topGenre: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGrey),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = YTRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Taste Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$likedCount", color = YTRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Liked", color = TextSilver, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$downloadCount", color = YTRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Offline", color = TextSilver, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = topGenre, color = YTRed, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "Primary Vibe", color = TextSilver, fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider(color = Color.White.copy(alpha = 0.08f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // AI generated aesthetic text
            val quote = when {
                likedCount == 0 && downloadCount == 0 -> "Your premium music journey is waiting. Customize your AI genre preferences on the Home page to compile mixes."
                topGenre.contains("Synthwave", ignoreCase = true) -> "Your vibe is cybernetic. You enjoy neon beats and midnight drives along digital boulevards."
                topGenre.contains("Lofi", ignoreCase = true) -> "Your vibe is mindful. Relaxing, dusty rhythms, rain on windowpanes, and warm coffee cups define your aesthetics."
                topGenre.contains("Pop", ignoreCase = true) -> "Your vibe is energetic. Upbeat tempos, catchy summer anthems, and modern vocal melodies power your mix."
                else -> "Your music aesthetic is diverse and sophisticated. Enjoy your curated, ad-free Premium playback."
            }
            
            Text(
                text = "\"$quote\"",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun DownloadsTab(
    songs: List<Song>,
    downloadProgress: Map<String, Int>,
    onPlay: (Song) -> Unit,
    onDelete: (Song) -> Unit
) {
    if (songs.isEmpty() && downloadProgress.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = TextSilver, modifier = Modifier.size(52.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Offline Downloads",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap the download icon while playing any song. Downloaded songs can be enjoyed completely offline, without internet connectivity.",
                    color = TextSilver,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Display active downloads in progress
            if (downloadProgress.isNotEmpty()) {
                item {
                    Text("Downloading...", color = YTRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                }
                
                items(downloadProgress.toList()) { (songId, progress) ->
                    ActiveDownloadRow(songId = songId, progress = progress)
                }
            }

            if (songs.isNotEmpty()) {
                item {
                    Text("Offline Tracks", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                }
                
                items(songs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(song) }
                            .padding(vertical = 8.dp)
                            .testTag("downloaded_row_${song.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DownloadDone, contentDescription = "Offline", tint = YTRed, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${song.artist} • ${song.genre}",
                                    color = TextSilver,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { onDelete(song) },
                            modifier = Modifier.testTag("delete_download_${song.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSilver, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ActiveDownloadRow(
    songId: String,
    progress: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardGrey, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            progress = progress / 100f,
            color = YTRed,
            trackColor = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.size(24.dp),
            strokeWidth = 3.dp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text("Downloading Track...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                color = YTRed,
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text("$progress%", color = YTRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun LikedSongsTab(
    songs: List<Song>,
    onPlay: (Song) -> Unit,
    onShuffle: () -> Unit,
    onToggleLike: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    downloadProgress: Map<String, Int>
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = TextSilver, modifier = Modifier.size(52.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Liked Songs Yet",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Touch the heart icon during active music playback to build your collection of favorites.",
                    color = TextSilver,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    } else {
        Column {
            // Shuffle Play button
            Button(
                onClick = onShuffle,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("shuffle_button")
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle Play", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(songs) { song ->
                    SongRowItem(
                        song = song,
                        onPlayClick = { onPlay(song) },
                        onLikeClick = { onToggleLike(song) },
                        onDownloadClick = { onDownload(song) },
                        downloadProgress = downloadProgress[song.id]
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
