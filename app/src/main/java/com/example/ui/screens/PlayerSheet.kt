package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardGrey
import com.example.ui.theme.TextSilver
import com.example.ui.theme.YTRed
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun PlayerSheet(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.playbackPosition.collectAsState()
    val durationMs by viewModel.playbackDuration.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }
    var offsetY by remember { mutableStateOf(0f) }

    val activeSong = currentSong ?: return

    // Layout configuration
    val playerHeight = if (isExpanded) {
        WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 800.dp
    } else {
        64.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -150f) {
                            isExpanded = true
                        } else if (offsetY > 150f) {
                            isExpanded = false
                        }
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount.y
                    }
                )
            }
            .offset { IntOffset(0, offsetY.toInt().coerceIn(if (isExpanded) 0 else -300, if (isExpanded) 1000 else 0)) }
            .background(if (isExpanded) AmoledBlack else Color.Transparent)
            .testTag("player_sheet")
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) { it } togetherWith
                slideOutVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) { it }
            },
            label = "player_expansion"
        ) { expanded ->
            if (expanded) {
                ExpandedPlayerContent(
                    song = activeSong,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    downloadProgress = downloadProgress[activeSong.id],
                    onCollapse = { isExpanded = false },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.skipNext() },
                    onPrev = { viewModel.skipPrevious() },
                    onSeek = { viewModel.seekTo(it) },
                    onLike = { viewModel.toggleLikeSong(activeSong) },
                    onDownload = { viewModel.downloadSong(activeSong) }
                )
            } else {
                CollapsedMiniPlayer(
                    song = activeSong,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onExpand = { isExpanded = true },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.skipNext() },
                    onSeek = { viewModel.seekTo(it) }
                )
            }
        }
    }
}

@Composable
fun CollapsedMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardGrey)
            .clickable { onExpand() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
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

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.testTag("mini_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("mini_skip_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Interactive scrub bar (Slider) with premium compact design
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = positionMs.toFloat().coerceAtMost(durationMs.toFloat()),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = YTRed,
                        activeTrackColor = YTRed,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mini_scrub_bar")
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun ExpandedPlayerContent(
    song: Song,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    downloadProgress: Int?,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Int) -> Unit,
    onLike: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLyrics by remember { mutableStateOf(false) }

    // Album art rotating animation
    val infiniteTransition = rememberInfiniteTransition(label = "album_art_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val rotation = if (isPlaying) angle else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF330006), AmoledBlack, AmoledBlack)
                )
            )
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Text(
                text = "NOW PLAYING FROM YOUTUBE",
                color = TextSilver,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            IconButton(onClick = { showLyrics = !showLyrics }) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = "Lyrics",
                    tint = if (showLyrics) YTRed else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showLyrics) {
            // Lyrics View Mode
            LyricsPanel(
                lyrics = song.lyrics ?: "No lyrics available for this premium track.",
                currentPositionMs = positionMs,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Standard Player View Mode
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Spinning Album Art
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Middle vinyl circle overlay
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Black, CircleShape)
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Beautiful glowing sound visualizer
                AudioWaveformVisualizer(isPlaying = isPlaying)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title and Subtitle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = TextSilver,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onLike) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (song.isLiked) YTRed else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Seek Bar
        Column {
            Slider(
                value = positionMs.toFloat().coerceAtMost(durationMs.toFloat()),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = YTRed,
                    activeTrackColor = YTRed,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(positionMs), color = TextSilver, fontSize = 11.sp)
                Text(text = formatTime(durationMs), color = TextSilver, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Premium Control Nodes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Offline Download Button
            Box(contentAlignment = Alignment.Center) {
                if (downloadProgress != null) {
                    CircularProgressIndicator(
                        progress = downloadProgress / 100f,
                        color = YTRed,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = if (song.isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            contentDescription = "Download Offline",
                            tint = if (song.isDownloaded) YTRed else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onPrev) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // Central Pulsing Play Button
            Button(
                onClick = onPlayPause,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = YTRed),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(64.dp)
                    .testTag("expanded_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            IconButton(onClick = {}) {
                Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LyricsPanel(
    lyrics: String,
    currentPositionMs: Int,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Interactive Lyrics",
                color = YTRed,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            lyrics.split("\n").forEach { line ->
                val lineTimeMs = parseLyricsTimecode(line)
                val isActive = lineTimeMs != -1 && currentPositionMs >= lineTimeMs && currentPositionMs <= lineTimeMs + 15000
                val cleanLine = line.replace(Regex("\\[\\d{2}:\\d{2}\\]\\s*"), "")
                
                Text(
                    text = cleanLine,
                    color = if (isActive) YTRed else Color.White.copy(alpha = 0.6f),
                    fontSize = if (isActive) 18.sp else 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Custom Rhythmic Waveform Canvas
@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                animationProgress += 0.1f
                delay(16) // ~60fps
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 24.dp)
    ) {
        val count = 24
        val spacing = size.width / count
        val barWidth = 4.dp.toPx()

        for (i in 0 until count) {
            val sinVal = if (isPlaying) {
                sin(animationProgress + i * 0.5f)
            } else {
                sin(i * 0.5f) * 0.2f
            }
            
            // Random-looking but cohesive heights
            val heightPercent = (sinVal + 1f) / 2f
            val barHeight = size.height * heightPercent * 0.8f + size.height * 0.2f

            val x = i * spacing + spacing / 2f
            val startY = size.height / 2f - barHeight / 2f
            val endY = size.height / 2f + barHeight / 2f

            drawLine(
                color = if (isPlaying) YTRed else Color.White.copy(alpha = 0.15f),
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = barWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

private fun formatTime(ms: Int): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun parseLyricsTimecode(line: String): Int {
    val regex = Regex("\\[(\\d{2}):(\\d{2})\\]")
    val matchResult = regex.find(line) ?: return -1
    val mins = matchResult.groupValues[1].toIntOrNull() ?: 0
    val secs = matchResult.groupValues[2].toIntOrNull() ?: 0
    return (mins * 60 + secs) * 1000
}
