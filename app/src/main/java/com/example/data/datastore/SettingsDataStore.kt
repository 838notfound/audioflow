package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "downloader_settings")

data class AppSettings(
    val audioFormat: String = "mp3",
    val audioQuality: String = "320k",
    val useAria2c: Boolean = false,
    val concurrentDownloads: Int = 1,
    val autoApproveMatches: Boolean = false,
    val addMetadata: Boolean = true,
    val embedThumbnail: Boolean = true,
    val lastKnownVersion: String = "yt-dlp core",
    val downloadBaseFolder: String = "Music",
    val downloadSubfolder: String = "AudioFlow"
)

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_AUDIO_FORMAT = stringPreferencesKey("audio_format")
        val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val KEY_USE_ARIA2C = booleanPreferencesKey("use_aria2c")
        val KEY_CONCURRENT_DOWNLOADS = intPreferencesKey("concurrent_downloads")
        val KEY_AUTO_APPROVE = booleanPreferencesKey("auto_approve")
        val KEY_ADD_METADATA = booleanPreferencesKey("add_metadata")
        val KEY_EMBED_THUMBNAIL = booleanPreferencesKey("embed_thumbnail")
        val KEY_LAST_KNOWN_VERSION = stringPreferencesKey("last_known_version")
        val KEY_DOWNLOAD_BASE_FOLDER = stringPreferencesKey("download_base_folder")
        val KEY_DOWNLOAD_SUBFOLDER = stringPreferencesKey("download_subfolder")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            audioFormat = preferences[KEY_AUDIO_FORMAT] ?: "mp3",
            audioQuality = preferences[KEY_AUDIO_QUALITY] ?: "320k",
            useAria2c = preferences[KEY_USE_ARIA2C] ?: false,
            concurrentDownloads = preferences[KEY_CONCURRENT_DOWNLOADS] ?: 1,
            autoApproveMatches = preferences[KEY_AUTO_APPROVE] ?: false,
            addMetadata = preferences[KEY_ADD_METADATA] ?: true,
            embedThumbnail = preferences[KEY_EMBED_THUMBNAIL] ?: true,
            lastKnownVersion = preferences[KEY_LAST_KNOWN_VERSION] ?: "yt-dlp core",
            downloadBaseFolder = preferences[KEY_DOWNLOAD_BASE_FOLDER] ?: "Music",
            downloadSubfolder = preferences[KEY_DOWNLOAD_SUBFOLDER] ?: "AudioFlow"
        )
    }

    suspend fun setAudioFormat(format: String) {
        context.dataStore.edit { it[KEY_AUDIO_FORMAT] = format.lowercase() }
    }

    suspend fun setAudioQuality(quality: String) {
        context.dataStore.edit { it[KEY_AUDIO_QUALITY] = quality }
    }

    suspend fun setUseAria2c(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_ARIA2C] = enabled }
    }

    suspend fun setConcurrentDownloads(count: Int) {
        context.dataStore.edit { it[KEY_CONCURRENT_DOWNLOADS] = count }
    }

    suspend fun setAutoApprove(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_APPROVE] = enabled }
    }

    suspend fun setAddMetadata(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ADD_METADATA] = enabled }
    }

    suspend fun setEmbedThumbnail(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EMBED_THUMBNAIL] = enabled }
    }

    suspend fun setLastKnownVersion(version: String) {
        context.dataStore.edit { it[KEY_LAST_KNOWN_VERSION] = version }
    }

    suspend fun setDownloadBaseFolder(baseFolder: String) {
        context.dataStore.edit { it[KEY_DOWNLOAD_BASE_FOLDER] = baseFolder }
    }

    suspend fun setDownloadSubfolder(subfolder: String) {
        val sanitized = subfolder.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val finalFolder = if (sanitized.isBlank()) "AudioFlow" else sanitized
        context.dataStore.edit { it[KEY_DOWNLOAD_SUBFOLDER] = finalFolder }
    }
}
