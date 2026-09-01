package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.MainApplication
import com.eightnf.audioflow.R
import com.example.data.datastore.AppSettings
import com.example.data.datastore.SettingsDataStore
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadDao
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.engine.MediaStoreHelper
import com.example.engine.YoutubeDownloadEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_QUEUE = "com.example.action.START_QUEUE"
        const val ACTION_CANCEL_ITEM = "com.example.action.CANCEL_ITEM"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val EXTRA_ITEM_ID = "extra_item_id"

        fun startQueue(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_QUEUE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelItem(context: Context, itemId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_ITEM
                putExtra(EXTRA_ITEM_ID, itemId)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadWorkerJob: Job? = null

    private lateinit var downloadDao: DownloadDao
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var engine: YoutubeDownloadEngine
    private lateinit var notificationManager: NotificationManager

    @Volatile
    private var currentActiveItemId: Long? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DownloadService created")
        downloadDao = AppDatabase.getDatabase(this).downloadDao()
        settingsDataStore = SettingsDataStore(this)
        engine = YoutubeDownloadEngine(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_QUEUE
        Log.d(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_START_QUEUE -> {
                startForegroundWithNotification("Starting download engine...", 0, "")
                startDownloadLoop()
            }
            ACTION_CANCEL_ITEM -> {
                val itemId = intent?.getLongExtra(EXTRA_ITEM_ID, -1L) ?: -1L
                if (itemId > 0) {
                    serviceScope.launch {
                        downloadDao.deleteItemById(itemId)
                    }
                }
            }
            ACTION_STOP_SERVICE -> {
                stopDownloadLoop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(
        title: String,
        progress: Int,
        subText: String,
        isIndeterminate: Boolean = false
    ) {
        val notification = buildNotification(title, progress, subText, isIndeterminate)
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun updateNotification(
        title: String,
        progress: Int,
        subText: String,
        isIndeterminate: Boolean = false
    ) {
        val notification = buildNotification(title, progress, subText, isIndeterminate)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        contentTitle: String,
        progress: Int,
        subText: String,
        isIndeterminate: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, MainApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(contentTitle)
            .setContentText(subText.ifEmpty { "Downloading audio tracks..." })
            .setProgress(100, progress, isIndeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun startDownloadLoop() {
        if (downloadWorkerJob?.isActive == true) {
            Log.d(TAG, "Download worker loop already active")
            return
        }

        downloadWorkerJob = serviceScope.launch {
            Log.d(TAG, "Worker loop started")

            while (isActive) {
                val nextItem = downloadDao.getNextQueuedItem()
                if (nextItem == null) {
                    Log.d(TAG, "No queued items found. Checking again in 2s...")
                    delay(2000)
                    // Check if still no queued items
                    if (downloadDao.getNextQueuedItem() == null) {
                        Log.d(TAG, "Queue empty. Shutting down foreground service.")
                        updateNotification("All downloads completed", 100, "Queue is empty", false)
                        delay(2000)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        break
                    }
                    continue
                }

                processItem(nextItem)
            }
        }
    }

    private suspend fun processItem(item: DownloadItem) {
        currentActiveItemId = item.id
        val settings: AppSettings = settingsDataStore.settingsFlow.first()

        Log.d(TAG, "Processing download item: ${item.id} - ${item.songTitle}")
        downloadDao.updateItem(
            item.copy(
                status = DownloadStatus.DOWNLOADING,
                progress = 5f,
                downloadSpeed = "Connecting...",
                errorMessage = null
            )
        )

        updateNotification("Downloading: ${item.songTitle}", 5, "Connecting...")

        try {
            var lastUpdateMillis = 0L
            val downloadedFile: File = engine.downloadAudio(item, settings) { progress, speed, eta, status ->
                val now = System.currentTimeMillis()
                if (now - lastUpdateMillis > 350 || progress >= 99f) {
                    lastUpdateMillis = now
                    serviceScope.launch {
                        downloadDao.updateItem(
                            item.copy(
                                status = status,
                                progress = progress,
                                downloadSpeed = speed,
                                eta = eta
                            )
                        )
                    }
                    val statusText = if (status == DownloadStatus.EXTRACTING_AUDIO) {
                        "Extracting audio to ${settings.audioFormat.uppercase()}..."
                    } else {
                        "$speed ${if (eta.isNotEmpty()) "• ETA $eta" else ""}".trim()
                    }
                    updateNotification("${item.songTitle} (${progress.toInt()}%)", progress.toInt(), statusText)
                }
            }

            // Successfully downloaded and converted to audio file
            Log.d(TAG, "Audio downloaded to ${downloadedFile.absolutePath}. Saving to MediaStore...")
            downloadDao.updateItem(
                item.copy(
                    status = DownloadStatus.EXTRACTING_AUDIO,
                    progress = 95f,
                    downloadSpeed = "Saving to MediaStore...",
                    fileSizeBytes = downloadedFile.length()
                )
            )

            val (mediaUri, relativePath) = MediaStoreHelper.saveAudioToMediaStore(
                context = applicationContext,
                sourceFile = downloadedFile,
                title = item.songTitle.ifEmpty { item.videoTitle },
                artist = item.artist.ifEmpty { item.channelName },
                format = settings.audioFormat
            )

            // Cleanup temp file
            try {
                downloadedFile.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete temporary download file: ${e.message}")
            }

            downloadDao.updateItem(
                item.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100f,
                    downloadSpeed = "",
                    eta = "",
                    mediaStoreUri = mediaUri?.toString(),
                    targetFilePath = relativePath,
                    fileSizeBytes = if (downloadedFile.length() > 0) downloadedFile.length() else item.fileSizeBytes,
                    audioFormat = settings.audioFormat,
                    audioQuality = settings.audioQuality
                )
            )

            updateNotification("Completed: ${item.songTitle}", 100, "Saved to Music folder")
            delay(500)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to download item ${item.id}", e)
            downloadDao.updateItem(
                item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download failed",
                    downloadSpeed = "",
                    eta = ""
                )
            )
            updateNotification("Failed: ${item.songTitle}", 0, e.localizedMessage ?: "Error")
            delay(1000)
        } finally {
            currentActiveItemId = null
        }
    }

    private fun stopDownloadLoop() {
        downloadWorkerJob?.cancel()
        downloadWorkerJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DownloadService destroyed")
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
