package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
fun HomeScreen(
    viewModel: MusicViewModel,
    onSongSelected: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyMixes by viewModel.dailyMixes.collectAsState()
    val isGeneratingMixes by viewModel.isGeneratingMixes.collectAsState()
    val favoriteGenres by viewModel.favoriteGenres.collectAsState()
    val recentSongs by viewModel.recentSongs.collectAsState()
    val defaultLibrary = viewModel.defaultLibrary
    val quickPicks by viewModel.quickPicks.collectAsState()
    
    var showGenreDialog by remember { mutableStateOf(false) }
    var showApkInfoDialog by remember { mutableStateOf(false) }

    // Auto-generate mixes on launch if empty
    LaunchedEffect(Unit) {
        if (dailyMixes.isEmpty()) {
            viewModel.generateDailyMixes()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = YTRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "YT Music",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Premium",
                            fontSize = 11.sp,
                            color = YTRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showApkInfoDialog = true },
                        modifier = Modifier.testTag("apk_info_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download APK", tint = YTRed)
                    }
                    IconButton(
                        onClick = { showGenreDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Preferences", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AmoledBlack
                )
            )
        },
        containerColor = AmoledBlack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero banner section
            item {
                HeroBanner(onCustomiseClick = { showGenreDialog = true })
            }

            // Gemini Curated Daily Mixes Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = YTRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Your Daily Mixes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        TextButton(
                            onClick = { viewModel.generateDailyMixes() },
                            colors = ButtonDefaults.textButtonColors(contentColor = YTRed),
                            enabled = !isGeneratingMixes
                        ) {
                            if (isGeneratingMixes) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = YTRed)
                            } else {
                                Text("Re-generate", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isGeneratingMixes && dailyMixes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = YTRed)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Gemini is curating your premium mix...", color = TextSilver, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dailyMixes) { song ->
                                MixCard(song = song, onClick = { onSongSelected(song, dailyMixes) })
                            }
                        }
                    }
                }
            }

            // Recently Played Section (Visible only if user played something)
            if (recentSongs.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Recently Played",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        recentSongs.take(4).forEach { song ->
                            SongRowItem(
                                song = song,
                                onPlayClick = { onSongSelected(song, recentSongs) },
                                onLikeClick = { viewModel.toggleLikeSong(song) }
                            )
                        }
                    }
                }
            }

            // Quick Picks / Default Premium Library (Dynamically Scraped from YouTube)
            item {
                Column {
                    Text(
                        text = "Quick Picks",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val displayLibrary = quickPicks.ifEmpty { defaultLibrary }
                    displayLibrary.forEach { song ->
                        SongRowItem(
                            song = song,
                            onPlayClick = { onSongSelected(song, displayLibrary) },
                            onLikeClick = { viewModel.toggleLikeSong(song) }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // Avoid player overlay overlap
            }
        }
    }

    // Genre customizer dialog
    if (showGenreDialog) {
        GenreSelectionDialog(
            currentGenres = favoriteGenres,
            onDismiss = { showGenreDialog = false },
            onSave = { selected ->
                viewModel.updateFavoriteGenres(selected)
                showGenreDialog = false
            }
        )
    }

    // APK Download Information Dialog
    if (showApkInfoDialog) {
        AlertDialog(
            onDismissRequest = { showApkInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = YTRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download APK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "To download the fully compiled APK for this music application:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Look at the Google AI Studio menu in your browser interface (located in the top-right header or settings/export panel).\n" +
                               "2. Open the project options/export menu.\n" +
                               "3. Select 'Export Project' or 'Download APK' to compile a direct installable package.\n" +
                               "4. Transfer or download the APK file onto your Android device to enjoy ad-free premium streaming offline anywhere!",
                        color = TextSilver,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showApkInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = YTRed)
                ) {
                    Text("Got It", color = Color.White)
                }
            },
            containerColor = CardGrey
        )
    }
}

@Composable
fun HeroBanner(
    onCustomiseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(YTRed, Color(0xFF660011), Color.Black)
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome to YT Music Premium",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Ad-free, offline listening, & smart mixes.",
                    color = TextSilver,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onCustomiseClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Customize AI Taste", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MixCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(140.dp)
            .clickable { onClick() }
            .testTag("mix_card_${song.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Overlay play button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(YTRed, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = TextSilver,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .padding(vertical = 8.dp)
            .testTag("song_row_${song.id}"),
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
                if (song.isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = YTRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = TextSilver,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.testTag("like_button_${song.id}")
        ) {
            Icon(
                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = if (song.isLiked) YTRed else TextSilver,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GenreSelectionDialog(
    currentGenres: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val allGenres = listOf("Lofi", "Synthwave", "Pop", "Jazz", "Electronic", "Rock", "Classical", "Hip Hop")
    val selected = remember { mutableStateListOf<String>().apply { addAll(currentGenres) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize AI Music Taste", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select your preferred genres. Gemini will compile your personalized daily mixes based on these categories.", color = TextSilver, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allGenres.forEach { genre ->
                        val isSelected = selected.contains(genre)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selected.remove(genre)
                                } else {
                                    selected.add(genre)
                                }
                            },
                            label = { Text(genre) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = TextSilver,
                                selectedContainerColor = YTRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = YTRed)
            ) {
                Text("Save & Generate", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSilver)
            }
        },
        containerColor = CardGrey
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
