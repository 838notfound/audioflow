package com.example.data.model

enum class DownloadStatus {
    STAGING,          // Waiting for user review / confirmation of match
    QUEUED,           // In the active download queue
    DOWNLOADING,      // Actively downloading video stream
    EXTRACTING_AUDIO, // Running FFmpeg audio extraction & conversion
    COMPLETED,        // Successfully tagged & saved to MediaStore Music
    FAILED,           // Download or extraction failed
    CANCELLED         // Cancelled by user
}
