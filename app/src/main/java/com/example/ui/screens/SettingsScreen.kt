package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.OtaUpdateDialog
import com.example.ui.theme.AmberSuccess
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val otaStatus by viewModel.otaStatus.collectAsStateWithLifecycle()
    val currentVersion by viewModel.currentYtDlpVersion.collectAsStateWithLifecycle()

    val formats = remember { listOf("MP3", "M4A", "OPUS", "FLAC", "WAV") }
    val qualities = remember {
        listOf(
            "320k" to "320 kbps (High Quality)",
            "256k" to "256 kbps (Standard High)",
            "192k" to "192 kbps (Medium)",
            "128k" to "128 kbps (Compact)",
            "best" to "Best Available (VBR)"
        )
    }

    var formatMenuExpanded by remember { mutableStateOf(false) }
    var qualityMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure audio formats, quality, and downloader engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Audio Encoding
        item {
            SettingsCategoryCard(title = "Audio Encoding & Format", icon = Icons.Default.Audiotrack) {
                // Format Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = formatMenuExpanded,
                    onExpandedChange = { formatMenuExpanded = !formatMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = settings.audioFormat.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Default Audio Format") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatMenuExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("audio_format_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = formatMenuExpanded,
                        onDismissRequest = { formatMenuExpanded = false }
                    ) {
                        formats.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format, fontWeight = if (settings.audioFormat.equals(format, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    viewModel.setAudioFormat(format)
                                    formatMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bitrate Selector Dropdown
                val currentQualityLabel = qualities.find { it.first == settings.audioQuality }?.second
                    ?: "${settings.audioQuality} (Custom)"

                ExposedDropdownMenuBox(
                    expanded = qualityMenuExpanded,
                    onExpandedChange = { qualityMenuExpanded = !qualityMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentQualityLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Bitrate / Audio Quality") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qualityMenuExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("audio_quality_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = qualityMenuExpanded,
                        onDismissRequest = { qualityMenuExpanded = false }
                    ) {
                        qualities.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontWeight = if (settings.audioQuality == key) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    viewModel.setAudioQuality(key)
                                    qualityMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Engine & Tagging
        item {
            SettingsCategoryCard(title = "Engine & Metadata", icon = Icons.Default.Tune) {
                // Aria2c multi-connection switch
                SettingsSwitchRow(
                    title = "Aria2c Multi-connection Acceleration",
                    subtitle = "Accelerate downloads with 4 concurrent segmented streams",
                    icon = Icons.Default.Bolt,
                    checked = settings.useAria2c,
                    onCheckedChange = { viewModel.setUseAria2c(it) },
                    testTag = "aria2c_switch"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Auto-approve matches switch
                SettingsSwitchRow(
                    title = "Auto-Approve Top Match",
                    subtitle = "Automatically enqueue top search match without review",
                    icon = Icons.Default.AutoAwesome,
                    checked = settings.autoApproveMatches,
                    onCheckedChange = { viewModel.setAutoApprove(it) },
                    testTag = "auto_approve_switch"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Metadata Embedding switch
                SettingsSwitchRow(
                    title = "Embed ID3 Metadata & Tags",
                    subtitle = "Embed track title, artist, album, and year into audio file",
                    icon = Icons.Default.LibraryMusic,
                    checked = settings.addMetadata,
                    onCheckedChange = { viewModel.setAddMetadata(it) },
                    testTag = "add_metadata_switch"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Embed Album Thumbnail switch
                SettingsSwitchRow(
                    title = "Embed Album Art Thumbnail",
                    subtitle = "Embed YouTube thumbnail as ID3 cover image (MP3/M4A)",
                    icon = Icons.Default.Image,
                    checked = settings.embedThumbnail,
                    onCheckedChange = { viewModel.setEmbedThumbnail(it) },
                    testTag = "embed_thumbnail_switch"
                )
            }
        }

        // Section 3: Storage & Scoped Storage
        item {
            SettingsCategoryCard(title = "Storage & MediaStore", icon = Icons.Default.Folder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save Location",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Music/SongDownloader/ (MediaStore Scoped Storage)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AmberSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Android 10+ Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberSuccess,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Section 4: OTA Binary Updater
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ota_updater_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "yt-dlp OTA Binary Updater",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Core version: $currentVersion",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Update the internal yt-dlp binary over-the-air to ensure continuous compatibility with YouTube format changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.triggerOtaUpdate() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_ota_update_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update yt-dlp Binaries")
                    }
                }
            }
        }
    }

    // OTA Dialog
    OtaUpdateDialog(
        status = otaStatus,
        onDismiss = { viewModel.dismissOtaDialog() }
    )
}

@Composable
fun SettingsCategoryCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
