package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.MusicRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerSheet
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.GenreSelectionDialog
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CardGrey
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextSilver
import com.example.ui.theme.YTRed
import com.example.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MusicViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Database and Repository Initialization
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MusicRepository(applicationContext, database.songDao())
        
        // Factory-based ViewModel injection
        viewModel = ViewModelProvider(this, MusicViewModel.Factory(repository))[MusicViewModel::class.java]
        
        // Bind to background audio service
        viewModel.bindPlaybackService(this)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(0) } // 0: Home, 1: Search, 2: Library
                val currentSong by viewModel.currentSong.collectAsState()
                var showGenreDialog by remember { mutableStateOf(false) }
                val favoriteGenres by viewModel.favoriteGenres.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()

                Scaffold(
                    topBar = {
                        Column(
                            modifier = Modifier
                                .background(AmoledBlack)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // YT Music Premium Logo
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { currentScreen = 0 }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = YTRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "YT Music",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Premium",
                                        fontSize = 10.sp,
                                        color = YTRed,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // Preferences Button
                                IconButton(
                                    onClick = { showGenreDialog = true },
                                    modifier = Modifier.testTag("global_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Preferences",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Global Search bar at the top of application to query songs, albums, and artists
                            TextField(
                                value = searchQuery,
                                onValueChange = {
                                    viewModel.setQuery(it)
                                    if (currentScreen != 1) {
                                        currentScreen = 1 // Auto-navigate to Search tab
                                    }
                                },
                                placeholder = { Text("Search songs, albums, or artists...", color = TextSilver, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
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
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("global_search_input")
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = AmoledBlack,
                            contentColor = YTRed,
                            modifier = Modifier
                                .height(80.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == 0,
                                onClick = { currentScreen = 0 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == 0) Icons.Default.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = { Text("Home", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = TextSilver,
                                    unselectedTextColor = TextSilver,
                                    indicatorColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("nav_home")
                            )

                            NavigationBarItem(
                                selected = currentScreen == 1,
                                onClick = { currentScreen = 1 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == 1) Icons.Default.Search else Icons.Outlined.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                label = { Text("Search", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = TextSilver,
                                    unselectedTextColor = TextSilver,
                                    indicatorColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("nav_search")
                            )

                            NavigationBarItem(
                                selected = currentScreen == 2,
                                onClick = { currentScreen = 2 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == 2) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                        contentDescription = "Library"
                                    )
                                },
                                label = { Text("Library", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = TextSilver,
                                    unselectedTextColor = TextSilver,
                                    indicatorColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("nav_library")
                            )
                        }
                    },
                    containerColor = AmoledBlack,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                    ) {
                        // Main Screen switching content area
                        when (currentScreen) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                onSongSelected = { song, queue ->
                                    viewModel.playSongFromList(song, queue)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            1 -> SearchScreen(
                                viewModel = viewModel,
                                onSongSelected = { song, queue ->
                                    viewModel.playSongFromList(song, queue)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            2 -> LibraryScreen(
                                viewModel = viewModel,
                                onSongSelected = { song, queue ->
                                    viewModel.playSongFromList(song, queue)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Sliding Player Overlay
                        if (currentSong != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                            ) {
                                PlayerSheet(viewModel = viewModel)
                            }
                        }
                    }

                    if (showGenreDialog) {
                        GenreSelectionDialog(
                            currentGenres = favoriteGenres,
                            onDismiss = { showGenreDialog = false },
                            onSave = { genres ->
                                viewModel.updateFavoriteGenres(genres)
                                showGenreDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun getAttributionTag(): String? {
        return "media"
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unbindPlaybackService(this)
    }
}
