package org.lumengallery.app.data.model

import android.net.Uri

/**
 * Model representing a physical folder/album containing media files.
 */
data class MediaAlbum(
    val id: Long,
    val name: String,
    val coverUri: Uri,
    val count: Int,
    val relativePath: String = ""
)
