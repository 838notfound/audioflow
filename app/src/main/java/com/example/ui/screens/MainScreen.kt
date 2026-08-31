package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.MainViewModel

enum class MainDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    QUEUE("Queue", Icons.Filled.Download, Icons.Outlined.Download),
    LIBRARY("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(MainDestination.QUEUE) }

    val stagingItems by viewModel.stagingItems.collectAsStateWithLifecycle()
    val activeQueue by viewModel.activeQueue.collectAsStateWithLifecycle()
    val completedItems by viewModel.completedItems.collectAsStateWithLifecycle()

    val totalQueueBadge = stagingItems.size + activeQueue.size
    val libraryBadge = completedItems.size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricIndigo,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Song Downloader",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "YouTube Audio Extractor • yt-dlp",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("main_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                MainDestination.values().forEach { destination ->
                    val isSelected = destination == currentDestination
                    val icon = if (isSelected) destination.selectedIcon else destination.unselectedIcon

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            when (destination) {
                                MainDestination.QUEUE -> {
                                    if (totalQueueBadge > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = NeonCyan,
                                                    contentColor = Color.Black
                                                ) {
                                                    Text("$totalQueueBadge", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        ) {
                                            Icon(icon, contentDescription = destination.title)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = destination.title)
                                    }
                                }
                                MainDestination.LIBRARY -> {
                                    if (libraryBadge > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.White
                                                ) {
                                                    Text("$libraryBadge")
                                                }
                                            }
                                        ) {
                                            Icon(icon, contentDescription = destination.title)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = destination.title)
                                    }
                                }
                                MainDestination.SETTINGS -> {
                                    Icon(icon, contentDescription = destination.title)
                                }
                            }
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentDestination,
            label = "screen_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { destination ->
            when (destination) {
                MainDestination.QUEUE -> QueueScreen(viewModel = viewModel)
                MainDestination.LIBRARY -> LibraryScreen(viewModel = viewModel)
                MainDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
