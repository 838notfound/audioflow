package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream

object MediaStoreHelper {
    private const val TAG = "MediaStoreHelper"

    fun saveAudioToMediaStore(
        context: Context,
        sourceFile: File,
        title: String,
        artist: String,
        format: String,
        baseFolder: String = "Music",
        subfolder: String = "AudioFlow"
    ): Pair<Uri?, String> {
        val mimeType = when (format.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "opus" -> "audio/opus"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            else -> "audio/*"
        }

        val sanitizedTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val displayName = if (sanitizedTitle.endsWith(".$format", ignoreCase = true)) {
            sanitizedTitle
        } else {
            "$sanitizedTitle.$format"
        }

        val cleanBaseFolder = if (baseFolder.isBlank()) "Music" else baseFolder.trim()
        val cleanSubfolder = if (subfolder.isBlank()) "AudioFlow" else subfolder.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val relativePath = "$cleanBaseFolder/$cleanSubfolder"

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.TITLE, title)
            if (artist.isNotBlank()) {
                put(MediaStore.Audio.Media.ARTIST, artist)
            }
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.SIZE, sourceFile.length())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                Log.d(TAG, "Successfully exported $displayName to MediaStore at $relativePath: $uri")
                return Pair(uri, "$relativePath/$displayName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving audio to MediaStore", e)
            if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.delete(uri, null, null)
            }
        }

        return Pair(null, sourceFile.absolutePath)
    }
}
