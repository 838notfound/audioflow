package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LibraryTrackCard
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val completedItems by viewModel.completedItems.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var filterQuery by remember { mutableStateOf("") }
    var selectedFormatFilter by remember { mutableStateOf("ALL") }

    val formatOptions = listOf("ALL", "MP3", "M4A", "OPUS", "FLAC", "WAV")

    val filteredList = completedItems.filter { item ->
        val matchesQuery = filterQuery.isBlank() ||
                item.songTitle.contains(filterQuery, ignoreCase = true) ||
                item.artist.contains(filterQuery, ignoreCase = true) ||
                item.videoTitle.contains(filterQuery, ignoreCase = true)

        val matchesFormat = selectedFormatFilter == "ALL" ||
                item.audioFormat.equals(selectedFormatFilter, ignoreCase = true)

        matchesQuery && matchesFormat
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Music Library",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${completedItems.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (completedItems.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearCompleted() },
                        modifier = Modifier.testTag("clear_completed_history_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear History", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Search Bar
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filter downloaded tracks...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search library",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear text")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Format Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(formatOptions) { format ->
                    FilterChip(
                        selected = selectedFormatFilter == format,
                        onClick = { selectedFormatFilter = format },
                        label = { Text(format, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_$format")
                    )
                }
            }
        }

        // Track List / Empty state
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (completedItems.isEmpty()) "No Downloaded Tracks Yet" else "No matching tracks found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (completedItems.isEmpty()) {
                            "Downloaded songs are tagged and saved into your device's Music folder."
                        } else {
                            "Try clearing or adjusting your search filters."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_track_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { "lib_${it.id}" }) { item ->
                    LibraryTrackCard(
                        item = item,
                        onPlay = { viewModel.playAudioFile(context, item) },
                        onShare = { viewModel.shareAudioFile(context, item) },
                        onDelete = { viewModel.cancelOrDeleteItem(item.id) }
                    )
                }
            }
        }
    }
}
