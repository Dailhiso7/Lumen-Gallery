package org.lumengallery.app.data.model

import android.net.Uri

/**
 * Immutable model representing a media asset (photo or video) loaded from MediaStore.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val dateTaken: Long,
    val dateModified: Long,
    val duration: Long = 0L, // in milliseconds, > 0 for videos
    val width: Int = 0,
    val height: Int = 0,
    val bucketId: Long = 0L,
    val bucketName: String = "",
    val isFavorite: Boolean = false
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    val formattedDuration: String
        get() {
            if (duration <= 0) return ""
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                "%d:%02d:%02d".format(hours, remainingMinutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }
}
