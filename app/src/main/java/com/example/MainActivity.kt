package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.MusicRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerSheet
import com.example.ui.screens.SearchScreen
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

                Scaffold(
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
                            .padding(bottom = innerPadding.calculateBottomPadding())
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
