package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.data.local.DownloadDao
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.SearchResultItem
import com.example.engine.YoutubeDownloadEngine
import com.example.service.DownloadService
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    companion object {
        private const val TAG = "DownloadRepository"
    }

    private val engine = YoutubeDownloadEngine(context)
    val settingsDataStore = SettingsDataStore(context)

    val stagingItems: Flow<List<DownloadItem>> = downloadDao.getStagingItems()
    val activeQueue: Flow<List<DownloadItem>> = downloadDao.getActiveQueue()
    val completedItems: Flow<List<DownloadItem>> = downloadDao.getCompletedItems()
    val failedItems: Flow<List<DownloadItem>> = downloadDao.getFailedItems()
    val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow

    suspend fun searchAndEnqueue(inputQuery: String): Result<DownloadItem> = withContext(Dispatchers.IO) {
        val query = inputQuery.trim()
        if (query.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Query cannot be empty"))

        try {
            val settings = settingsFlow.first()
            val results = engine.searchTracks(query, count = 5)
            if (results.isEmpty()) {
                return@withContext Result.failure(Exception("No matching tracks found for '$query'"))
            }

            val topMatch = results.first()
            val alternativeMatchesJson = serializeSearchResults(results)

            val parsedArtist = if (topMatch.channel.isNotBlank() && topMatch.channel != "YouTube") {
                topMatch.channel
            } else {
                extractArtistFromTitle(topMatch.title)
            }

            val item = DownloadItem(
                query = query,
                songTitle = topMatch.title,
                artist = parsedArtist,
                youtubeVideoId = topMatch.videoId,
                youtubeUrl = topMatch.url,
                videoTitle = topMatch.title,
                channelName = topMatch.channel,
                duration = topMatch.duration,
                thumbnailUrl = topMatch.thumbnailUrl,
                matchConfidence = topMatch.confidence,
                alternativeMatchesJson = alternativeMatchesJson,
                status = if (settings.autoApproveMatches) DownloadStatus.QUEUED else DownloadStatus.STAGING,
                audioFormat = settings.audioFormat,
                audioQuality = settings.audioQuality
            )

            val generatedId = downloadDao.insertItem(item)
            val savedItem = item.copy(id = generatedId)

            if (settings.autoApproveMatches) {
                DownloadService.startQueue(context)
            }

            Result.success(savedItem)
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchAndEnqueue", e)
            Result.failure(e)
        }
    }

    suspend fun batchSearchAndEnqueue(queries: List<String>): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        val settings = settingsFlow.first()

        for (q in queries) {
            val clean = q.trim()
            if (clean.isNotBlank()) {
                val res = searchAndEnqueue(clean)
                if (res.isSuccess) {
                    addedCount++
                }
            }
        }

        if (settings.autoApproveMatches && addedCount > 0) {
            DownloadService.startQueue(context)
        }
        addedCount
    }

    suspend fun selectAlternativeMatch(itemId: Long, selected: SearchResultItem) = withContext(Dispatchers.IO) {
        val item = downloadDao.getItemById(itemId) ?: return@withContext
        val updated = item.copy(
            youtubeVideoId = selected.videoId,
            youtubeUrl = selected.url,
            videoTitle = selected.title,
            songTitle = selected.title,
            channelName = selected.channel,
            artist = if (selected.channel.isNotBlank() && selected.channel != "YouTube") selected.channel else extractArtistFromTitle(selected.title),
            duration = selected.duration,
            thumbnailUrl = selected.thumbnailUrl,
            matchConfidence = selected.confidence
        )
        downloadDao.updateItem(updated)
    }

    suspend fun approveItem(itemId: Long) = withContext(Dispatchers.IO) {
        val item = downloadDao.getItemById(itemId) ?: return@withContext
        val updated = item.copy(status = DownloadStatus.QUEUED, errorMessage = null)
        downloadDao.updateItem(updated)
        DownloadService.startQueue(context)
    }

    suspend fun approveAllStaging() = withContext(Dispatchers.IO) {
        downloadDao.approveAllStaging()
        DownloadService.startQueue(context)
    }

    suspend fun retryItem(itemId: Long) = withContext(Dispatchers.IO) {
        downloadDao.retryItem(itemId)
        DownloadService.startQueue(context)
    }

    suspend fun cancelOrDeleteItem(itemId: Long) = withContext(Dispatchers.IO) {
        DownloadService.cancelItem(context, itemId)
        downloadDao.deleteItemById(itemId)
    }

    suspend fun clearCompleted() = withContext(Dispatchers.IO) {
        downloadDao.clearCompleted()
    }

    suspend fun clearStaging() = withContext(Dispatchers.IO) {
        downloadDao.clearStaging()
    }

    suspend fun updateYtDlpBinary(channel: YoutubeDL.UpdateChannel = YoutubeDL.UpdateChannel.STABLE): Pair<Boolean, String> {
        val result = engine.updateYtDlp(channel)
        if (result.first) {
            val ver = engine.getVersion() ?: "Latest"
            settingsDataStore.setLastKnownVersion(ver)
        }
        return result
    }

    fun getYtDlpVersion(): String {
        return engine.getVersion() ?: "yt-dlp core"
    }

    private fun extractArtistFromTitle(title: String): String {
        if (title.contains(" - ")) {
            val parts = title.split(" - ")
            return parts[0].trim()
        }
        return "Unknown Artist"
    }

    fun parseSearchResults(json: String): List<SearchResultItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SearchResultItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SearchResultItem(
                        videoId = obj.optString("videoId", ""),
                        title = obj.optString("title", ""),
                        channel = obj.optString("channel", ""),
                        duration = obj.optString("duration", ""),
                        thumbnailUrl = obj.optString("thumbnailUrl", ""),
                        url = obj.optString("url", ""),
                        confidence = obj.optInt("confidence", 90)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeSearchResults(results: List<SearchResultItem>): String {
        val arr = JSONArray()
        for (r in results) {
            val obj = JSONObject().apply {
                put("videoId", r.videoId)
                put("title", r.title)
                put("channel", r.channel)
                put("duration", r.duration)
                put("thumbnailUrl", r.thumbnailUrl)
                put("url", r.url)
                put("confidence", r.confidence)
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
