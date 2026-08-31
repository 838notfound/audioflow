package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.repository.DownloadRepository
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { DownloadRepository(this, database.downloadDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()
        initializeYoutubeDl()
    }

    private fun initializeYoutubeDl() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Initializing YoutubeDL, FFmpeg, and Aria2c...")
                YoutubeDL.getInstance().init(applicationContext)
                FFmpeg.getInstance().init(applicationContext)
                try {
                    Aria2c.getInstance().init(applicationContext)
                } catch (e: Exception) {
                    Log.w(TAG, "Aria2c init notice: ${e.message}")
                }
                isInitialized = true
                Log.d(TAG, "YoutubeDL successfully initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize YoutubeDL", e)
                initializationError = e.localizedMessage ?: "Unknown initialization error"
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "song_downloader_channel"
        const val TAG = "SongDownloaderApp"

        @Volatile
        var isInitialized: Boolean = false
            private set

        @Volatile
        var initializationError: String? = null
            private set

        lateinit var instance: MainApplication
            private set
    }
}
