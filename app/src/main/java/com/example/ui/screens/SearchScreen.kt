package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardGrey
import com.example.ui.theme.TextSilver
import com.example.ui.theme.YTRed
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onSongSelected: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val focusManager = LocalFocusManager.current
    
    val moodChips = listOf("Relax", "Focus lofi", "Workout pop", "Synthwave drive", "Jazz rain")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(AmoledBlack)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Input
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setQuery(it) },
                    placeholder = { Text("Search songs, albums, or artists...", color = TextSilver, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardGrey,
                        unfocusedContainerColor = CardGrey,
                        disabledContainerColor = CardGrey,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = YTRed,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("search_input")
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Suggestion Mood Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moodChips) { chip ->
                        val isSelected = searchQuery.equals(chip, ignoreCase = true)
                        SuggestionChip(
                            onClick = {
                                if (isSelected) {
                                    viewModel.setQuery("")
                                } else {
                                    viewModel.setQuery(chip)
                                    focusManager.clearFocus()
                                }
                            },
                            label = { Text(chip, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) YTRed else Color.White.copy(alpha = 0.1f),
                                labelColor = if (isSelected) Color.White else TextSilver
                            ),
                            border = null,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        },
        containerColor = AmoledBlack,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Ad-free Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1E1E1E), Color(0xFF121212))
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = YTRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Ad-Free Premium Playback", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("All YouTube tracks play uninterrupted.", color = TextSilver, fontSize = 11.sp)
                    }
                }
            }

            Text(
                text = if (searchQuery.isEmpty()) "Quick Hits" else "YouTube Results",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSilver, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching YouTube songs found", color = TextSilver, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchResults) { song ->
                        SongRowItem(
                            song = song,
                            onPlayClick = { onSongSelected(song, searchResults) },
                            onLikeClick = { viewModel.toggleLikeSong(song) }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Avoid player overlap
                    }
                }
            }
        }
    }
}
