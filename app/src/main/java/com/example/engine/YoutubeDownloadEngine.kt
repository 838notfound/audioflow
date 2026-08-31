package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.datastore.AppSettings
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.SearchResultItem
import com.yausername.youtubedl_android.DownloadProgressCallback
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class YoutubeDownloadEngine(private val context: Context) {

    companion object {
        private const val TAG = "YoutubeDownloadEngine"
    }

    suspend fun searchTracks(query: String, count: Int = 5): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        // Attempt 1: Native yt-dlp search via YoutubeDL execute
        try {
            val results = searchWithYtDlp(trimmed, count)
            if (results.isNotEmpty()) {
                Log.d(TAG, "Search via yt-dlp successful: found ${results.size} tracks")
                return@withContext results
            }
        } catch (e: Exception) {
            Log.w(TAG, "yt-dlp search attempt failed (${e.message}), trying lightweight fallback", e)
        }

        // Attempt 2: High-speed lightweight YouTube search fallback
        try {
            val fallbackResults = searchWithLightweightWeb(trimmed, count)
            if (fallbackResults.isNotEmpty()) {
                Log.d(TAG, "Search via web client successful: found ${fallbackResults.size} tracks")
                return@withContext fallbackResults
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lightweight web search failed", e)
        }

        // Return a baseline search candidate from query if both fail
        listOf(
            SearchResultItem(
                videoId = "",
                title = trimmed,
                channel = "YouTube Search",
                duration = "Auto",
                thumbnailUrl = "",
                url = if (trimmed.startsWith("http")) trimmed else "https://www.youtube.com/results?search_query=${URLEncoder.encode(trimmed, "UTF-8")}",
                confidence = 80
            )
        )
    }

    private fun searchWithYtDlp(query: String, count: Int): List<SearchResultItem> {
        val request = YoutubeDLRequest("ytsearch$count:$query").apply {
            addOption("--dump-single-json")
            addOption("--flat-playlist")
            addOption("--no-warnings")
            addOption("--ignore-errors")
            addOption("--no-playlist")
        }

        val response: YoutubeDLResponse = YoutubeDL.getInstance().execute(request)
        val out = response.out ?: return emptyList()

        val results = mutableListOf<SearchResultItem>()
        val json = JSONObject(out)
        val entries = json.optJSONArray("entries") ?: JSONArray()

        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val id = entry.optString("id", "")
            val title = entry.optString("title", query)
            val uploader = entry.optString("uploader", entry.optString("channel", "Unknown Artist"))
            val durationSec = entry.optInt("duration", 0)
            val durationStr = formatDuration(durationSec)
            val url = entry.optString("url", if (id.isNotEmpty()) "https://www.youtube.com/watch?v=$id" else "")
            val thumbnail = entry.optString("thumbnail", if (id.isNotEmpty()) "https://i.ytimg.com/vi/$id/hqdefault.jpg" else "")

            // Heuristic match confidence
            val confidence = calculateConfidence(query, title, uploader, i)

            results.add(
                SearchResultItem(
                    videoId = id,
                    title = title,
                    channel = uploader,
                    duration = durationStr,
                    durationSeconds = durationSec,
                    thumbnailUrl = thumbnail,
                    url = if (url.startsWith("http")) url else "https://www.youtube.com/watch?v=$id",
                    confidence = confidence
                )
            )
        }

        return results
    }

    private fun searchWithLightweightWeb(query: String, count: Int): List<SearchResultItem> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://www.youtube.com/results?search_query=$encoded"
        val url = URL(searchUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }

        val content = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        val initialDataRegex = Regex("var ytInitialData = (\\{.*?\\});</script>", RegexOption.DOT_MATCHES_ALL)
        val match = initialDataRegex.find(content)
        val results = mutableListOf<SearchResultItem>()

        if (match != null) {
            val jsonStr = match.groupValues[1]
            val root = JSONObject(jsonStr)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val section = contents.optJSONObject(i)?.optJSONObject("itemSectionRenderer")
                    val items = section?.optJSONArray("contents") ?: continue

                    for (j in 0 until items.length()) {
                        if (results.size >= count) break
                        val video = items.optJSONObject(j)?.optJSONObject("videoRenderer") ?: continue
                        val videoId = video.optString("videoId", "")
                        if (videoId.isEmpty()) continue

                        val title = video.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: video.optJSONObject("title")?.optString("simpleText", "") ?: query

                        val channel = video.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: video.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "YouTube"

                        val duration = video.optJSONObject("lengthText")?.optString("simpleText", "3:30") ?: "3:30"
                        val thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                        val confidence = calculateConfidence(query, title, channel, results.size)
                        results.add(
                            SearchResultItem(
                                videoId = videoId,
                                title = title,
                                channel = channel,
                                duration = duration,
                                thumbnailUrl = thumb,
                                url = "https://www.youtube.com/watch?v=$videoId",
                                confidence = confidence
                            )
                        )
                    }
                }
            }
        }

        // Secondary fallback regex if json parsing fails
        if (results.isEmpty()) {
            val videoIdRegex = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"")
            val titleRegex = Regex("\"title\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"\\}\\]")
            val matches = videoIdRegex.findAll(content).toList()

            for (m in matches.take(count)) {
                val vid = m.groupValues[1]
                if (results.none { it.videoId == vid }) {
                    results.add(
                        SearchResultItem(
                            videoId = vid,
                            title = query,
                            channel = "YouTube",
                            duration = "3:30",
                            thumbnailUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                            url = "https://www.youtube.com/watch?v=$vid",
                            confidence = 90 - (results.size * 3)
                        )
                    )
                }
            }
        }

        return results
    }

    private fun calculateConfidence(query: String, title: String, channel: String, index: Int): Int {
        val qWords = query.lowercase().split(Regex("[^a-zA-Z0-9]")).filter { it.isNotBlank() }
        val tLower = title.lowercase()
        val cLower = channel.lowercase()

        var matchedWords = 0
        for (w in qWords) {
            if (tLower.contains(w) || cLower.contains(w)) {
                matchedWords++
            }
        }

        val baseScore = if (qWords.isNotEmpty()) {
            (matchedWords.toFloat() / qWords.size * 50).toInt() + 45
        } else {
            75
        }

        val penaltyForRank = index * 4
        return (baseScore - penaltyForRank).coerceIn(40, 99)
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "--:--"
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }

    suspend fun downloadAudio(
        item: DownloadItem,
        settings: AppSettings,
        onProgress: (progress: Float, speed: String, eta: String, status: DownloadStatus) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val downloadDir = File(context.cacheDir, "yt_downloads").apply { mkdirs() }
        val processId = UUID.randomUUID().toString()

        val targetFormat = settings.audioFormat.lowercase()
        val targetQuality = when (settings.audioQuality) {
            "best" -> "0"
            "320k" -> "320k"
            "256k" -> "256k"
            "192k" -> "192k"
            "128k" -> "128k"
            else -> settings.audioQuality
        }

        val filenameTemplate = "${downloadDir.absolutePath}/%(id)s_audio.%(ext)s"

        val request = YoutubeDLRequest(item.youtubeUrl.ifEmpty { "https://www.youtube.com/watch?v=${item.youtubeVideoId}" }).apply {
            addOption("-x")
            addOption("--audio-format", targetFormat)
            addOption("--audio-quality", targetQuality)
            if (settings.addMetadata) {
                addOption("--add-metadata")
            }
            if (settings.embedThumbnail && (targetFormat == "mp3" || targetFormat == "m4a")) {
                addOption("--embed-thumbnail")
            }
            addOption("--no-mtime")
            addOption("--no-playlist")
            addOption("-o", filenameTemplate)

            if (settings.useAria2c) {
                try {
                    addOption("--external-downloader", "aria2c")
                    addOption("--external-downloader-args", "aria2c:\"-j 4 -x 4 -s 4 -k 1M\"")
                } catch (e: Exception) {
                    Log.w(TAG, "Aria2c option ignored: ${e.message}")
                }
            }
        }

        var currentStatus = DownloadStatus.DOWNLOADING
        onProgress(5f, "Connecting...", "00:00", currentStatus)

        val progressCallback: (Float, Long, String) -> Unit = { progress, etaInSeconds, line ->
            val cleanLine = line ?: ""
            var speedStr = ""
            if (cleanLine.contains("[download]") || cleanLine.contains("at")) {
                val speedMatch = Regex("at\\s+([0-9.]+\\s*[kMG]i?B/s)").find(cleanLine)
                if (speedMatch != null) {
                    speedStr = speedMatch.groupValues[1]
                }
            }

            if (cleanLine.contains("[ExtractAudio]") || cleanLine.contains("[ffmpeg]")) {
                currentStatus = DownloadStatus.EXTRACTING_AUDIO
            }

            val etaStr = if (etaInSeconds > 0L) {
                "%02d:%02d".format(etaInSeconds / 60, etaInSeconds % 60)
            } else ""

            onProgress(progress.coerceIn(0f, 100f), speedStr, etaStr, currentStatus)
        }

        onProgress(10f, "Downloading stream...", "", DownloadStatus.DOWNLOADING)
        val response = YoutubeDL.getInstance().execute(request, processId, progressCallback)

        Log.d(TAG, "YoutubeDL execute completed.")

        // Look for the generated output file in downloadDir
        val expectedPrefix = "${item.youtubeVideoId.ifEmpty { item.id.toString() }}_audio"
        val matchedFiles = downloadDir.listFiles { _, name ->
            name.contains(expectedPrefix) || name.endsWith(".$targetFormat")
        }?.sortedByDescending { it.lastModified() }

        val finalFile = matchedFiles?.firstOrNull()
            ?: downloadDir.listFiles()?.maxByOrNull { it.lastModified() }
            ?: throw YoutubeDLException("Downloaded audio file could not be found on disk.")

        Log.d(TAG, "Found output file: ${finalFile.absolutePath} (${finalFile.length()} bytes)")
        finalFile
    }

    suspend fun updateYtDlp(channel: YoutubeDL.UpdateChannel = YoutubeDL.UpdateChannel.STABLE): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext, channel)
            val version = getVersion() ?: "Updated"
            Log.d(TAG, "Updated YoutubeDL: status=$status, version=$version")
            Pair(true, "Updated to $version ($status)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update YoutubeDL binary", e)
            Pair(false, e.localizedMessage ?: "Update failed")
        }
    }

    fun getVersion(): String? {
        return try {
            YoutubeDL.getInstance().version(context.applicationContext)
        } catch (e: Exception) {
            null
        }
    }
}
