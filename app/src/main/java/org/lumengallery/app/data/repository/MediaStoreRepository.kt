package org.lumengallery.app.data.repository

import android.app.Activity
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.lumengallery.app.data.local.FavoriteDao
import org.lumengallery.app.data.model.MediaAlbum
import org.lumengallery.app.data.model.MediaItem

class MediaStoreRepository(
    private val context: Context,
    private val favoriteDao: FavoriteDao
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Flow that emits an event whenever MediaStore data changes on the device.
     */
    fun mediaStoreChangeFlow(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        // Send initial event
        trySend(Unit)

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    /**
     * Queries all images and videos from MediaStore, sorted descending by date.
     */
    suspend fun queryAllMedia(bucketId: Long? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val favoriteIds = favoriteDao.getAllFavoriteIds().toSet()
        val mediaList = mutableListOf<MediaItem>()

        val collectionUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        val selection = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        selection.append("(")
        selection.append("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?")
        selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
        selection.append(" OR ")
        selection.append("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?")
        selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        selection.append(")")

        if (bucketId != null) {
            selection.append(" AND ${MediaStore.MediaColumns.BUCKET_ID} = ?")
            selectionArgs.add(bucketId.toString())
        }

        // Exclude pending/trashed files on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection.append(" AND ${MediaStore.MediaColumns.IS_PENDING} = 0")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            selection.append(" AND ${MediaStore.MediaColumns.IS_TRASHED} = 0")
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                collectionUri,
                projection,
                selection.toString(),
                selectionArgs.toTypedArray(),
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val mediaType = cursor.getInt(mediaTypeCol)
                    val contentUri = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val displayName = cursor.getString(nameCol) ?: "Media_$id"
                    val mimeType = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val dateModified = if (dateModCol != -1) cursor.getLong(dateModCol) else 0L
                    var dateTaken = if (dateTakenCol != -1) cursor.getLong(dateTakenCol) else 0L
                    if (dateTaken <= 0L) {
                        dateTaken = dateAdded * 1000L
                    }

                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val bId = if (bucketIdCol != -1) cursor.getLong(bucketIdCol) else 0L
                    val bName = if (bucketNameCol != -1) cursor.getString(bucketNameCol) ?: "" else ""

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            path = path,
                            displayName = displayName,
                            mimeType = mimeType,
                            size = size,
                            dateTaken = dateTaken,
                            dateModified = dateModified,
                            duration = duration,
                            width = width,
                            height = height,
                            bucketId = bId,
                            bucketName = bName,
                            isFavorite = favoriteIds.contains(id)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaList
    }

    /**
     * Groups media into albums based on directory buckets.
     */
    suspend fun queryAlbums(): List<MediaAlbum> = withContext(Dispatchers.IO) {
        val allMedia = queryAllMedia()
        val albumMap = linkedMapOf<Long, MutableList<MediaItem>>()

        for (item in allMedia) {
            val bucketId = item.bucketId
            val list = albumMap.getOrPut(bucketId) { mutableListOf() }
            list.add(item)
        }

        albumMap.map { (bucketId, items) ->
            val first = items.first()
            MediaAlbum(
                id = bucketId,
                name = first.bucketName.ifBlank { "Internal Storage" },
                coverUri = first.uri,
                count = items.size,
                relativePath = first.path.substringBeforeLast('/', "")
            )
        }.sortedByDescending { it.count }
    }

    /**
     * Requests moving media items to system Trash.
     * On Android 11+ (API 30+), returns a PendingIntent for user confirmation.
     */
    fun createDeletePendingIntent(uris: List<Uri>): PendingIntent? = createMoveToTrashPendingIntent(uris)

    fun createMoveToTrashPendingIntent(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createTrashRequest(contentResolver, uris, true)
        } else {
            null
        }
    }

    /**
     * Restores media items from the Trash back to the gallery.
     */
    fun createRestoreFromTrashPendingIntent(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createTrashRequest(contentResolver, uris, false)
        } else {
            null
        }
    }

    /**
     * Permanently deletes media items from the device.
     */
    fun createDeletePermanentlyPendingIntent(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(contentResolver, uris)
        } else {
            null
        }
    }

    /**
     * Queries all trashed images and videos from MediaStore (Android 11+).
     */
    suspend fun queryTrashMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext emptyList()
        }

        val mediaList = mutableListOf<MediaItem>()
        val collectionUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )

        val bundle = android.os.Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )
        }

        try {
            contentResolver.query(collectionUri, projection, bundle, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val mediaType = cursor.getInt(mediaTypeCol)
                    val contentUri = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val displayName = cursor.getString(nameCol) ?: "Trash_$id"
                    val mimeType = cursor.getString(mimeCol) ?: "image/*"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val dateModified = if (dateModCol != -1) cursor.getLong(dateModCol) else 0L
                    var dateTaken = if (dateTakenCol != -1) cursor.getLong(dateTakenCol) else 0L
                    if (dateTaken <= 0L) {
                        dateTaken = dateAdded * 1000L
                    }

                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            path = path,
                            displayName = displayName,
                            mimeType = mimeType,
                            size = size,
                            dateTaken = dateTaken,
                            dateModified = dateModified,
                            duration = duration,
                            width = width,
                            height = height,
                            bucketId = -1L,
                            bucketName = "Trash",
                            isFavorite = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaList
    }

    /**
     * Fallback direct deletion for Android 10 or lower, or after permission granted.
     */
    suspend fun deleteDirectly(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rows = contentResolver.delete(uri, null, null)
            if (rows > 0) Result.success(Unit)
            else Result.failure(Exception("Failed to delete media item"))
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rename file displayName via MediaStore.
     */
    suspend fun renameMedia(uri: Uri, newName: String): Result<PendingIntent?> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
            }
            contentResolver.update(uri, values, null, null)
            Result.success(null)
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                Result.success(e.userAction.actionIntent)
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
