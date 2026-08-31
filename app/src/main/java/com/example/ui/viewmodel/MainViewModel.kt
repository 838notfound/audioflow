package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import com.example.data.datastore.AppSettings
import com.example.data.model.DownloadItem
import com.example.data.model.SearchResultItem
import com.example.data.repository.DownloadRepository
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OtaUpdateStatus {
    object Idle : OtaUpdateStatus
    data class Updating(val message: String = "Downloading latest yt-dlp binaries...") : OtaUpdateStatus
    data class Success(val message: String) : OtaUpdateStatus
    data class Error(val error: String) : OtaUpdateStatus
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadRepository = (application as MainApplication).repository

    val stagingItems: StateFlow<List<DownloadItem>> = repository.stagingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQueue: StateFlow<List<DownloadItem>> = repository.activeQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedItems: StateFlow<List<DownloadItem>> = repository.completedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val failedItems: StateFlow<List<DownloadItem>> = repository.failedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private val _showBatchDialog = MutableStateFlow(false)
    val showBatchDialog = _showBatchDialog.asStateFlow()

    private val _rematchItem = MutableStateFlow<DownloadItem?>(null)
    val rematchItem = _rematchItem.asStateFlow()

    private val _otaStatus = MutableStateFlow<OtaUpdateStatus>(OtaUpdateStatus.Idle)
    val otaStatus = _otaStatus.asStateFlow()

    private val _currentYtDlpVersion = MutableStateFlow(repository.getYtDlpVersion())
    val currentYtDlpVersion = _currentYtDlpVersion.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitSearch(query: String = _searchQuery.value) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || _isSearching.value) return

        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            val result = repository.searchAndEnqueue(trimmed)
            _isSearching.value = false

            result.onSuccess {
                _searchQuery.value = ""
            }.onFailure { error ->
                _searchError.value = error.localizedMessage ?: "Failed to find matching track"
            }
        }
    }

    fun submitBatchSearch(input: String) {
        if (input.isBlank()) return
        val lines = input.split(Regex("[\n,;]+")).map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        viewModelScope.launch {
            _isSearching.value = true
            _showBatchDialog.value = false
            val count = repository.batchSearchAndEnqueue(lines)
            _isSearching.value = false
            Toast.makeText(getApplication(), "Found & added $count tracks to staging", Toast.LENGTH_SHORT).show()
        }
    }

    fun openBatchDialog() {
        _showBatchDialog.value = true
    }

    fun closeBatchDialog() {
        _showBatchDialog.value = false
    }

    fun openRematchSheet(item: DownloadItem) {
        _rematchItem.value = item
    }

    fun closeRematchSheet() {
        _rematchItem.value = null
    }

    fun selectAlternativeMatch(itemId: Long, selected: SearchResultItem) {
        viewModelScope.launch {
            repository.selectAlternativeMatch(itemId, selected)
            _rematchItem.value = null
        }
    }

    fun approveItem(itemId: Long) {
        viewModelScope.launch {
            repository.approveItem(itemId)
        }
    }

    fun approveAllStaging() {
        viewModelScope.launch {
            repository.approveAllStaging()
        }
    }

    fun retryItem(itemId: Long) {
        viewModelScope.launch {
            repository.retryItem(itemId)
        }
    }

    fun cancelOrDeleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.cancelOrDeleteItem(itemId)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    fun clearStaging() {
        viewModelScope.launch {
            repository.clearStaging()
        }
    }

    fun clearSearchError() {
        _searchError.value = null
    }

    // Settings actions
    fun setAudioFormat(format: String) {
        viewModelScope.launch {
            repository.settingsDataStore.setAudioFormat(format)
        }
    }

    fun setAudioQuality(quality: String) {
        viewModelScope.launch {
            repository.settingsDataStore.setAudioQuality(quality)
        }
    }

    fun setUseAria2c(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsDataStore.setUseAria2c(enabled)
        }
    }

    fun setAutoApprove(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsDataStore.setAutoApprove(enabled)
        }
    }

    fun setAddMetadata(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsDataStore.setAddMetadata(enabled)
        }
    }

    fun setEmbedThumbnail(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsDataStore.setEmbedThumbnail(enabled)
        }
    }

    // OTA Updater
    fun triggerOtaUpdate() {
        viewModelScope.launch {
            _otaStatus.value = OtaUpdateStatus.Updating("Checking and downloading latest yt-dlp binaries...")
            val (success, message) = repository.updateYtDlpBinary(YoutubeDL.UpdateChannel.STABLE)
            if (success) {
                _otaStatus.value = OtaUpdateStatus.Success(message)
                _currentYtDlpVersion.value = repository.getYtDlpVersion()
            } else {
                _otaStatus.value = OtaUpdateStatus.Error(message)
            }
        }
    }

    fun dismissOtaDialog() {
        _otaStatus.value = OtaUpdateStatus.Idle
    }

    fun playAudioFile(context: Context, item: DownloadItem) {
        try {
            val uri = if (!item.mediaStoreUri.isNullOrEmpty()) {
                Uri.parse(item.mediaStoreUri)
            } else {
                null
            }

            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "audio/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Play audio with"))
            } else {
                Toast.makeText(context, "Audio file saved in Music/SongDownloader", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open audio player: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareAudioFile(context: Context, item: DownloadItem) {
        try {
            val uri = if (!item.mediaStoreUri.isNullOrEmpty()) {
                Uri.parse(item.mediaStoreUri)
            } else {
                null
            }
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share audio track"))
            } else {
                Toast.makeText(context, "File path unavailable", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun parseAlternatives(json: String): List<SearchResultItem> {
        return repository.parseSearchResults(json)
    }
}
