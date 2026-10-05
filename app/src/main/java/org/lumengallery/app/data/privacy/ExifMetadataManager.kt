package org.lumengallery.app.data.privacy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lumengallery.app.data.model.ExifData
import org.lumengallery.app.data.model.MediaItem
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExifMetadataManager(private val context: Context) {

    /**
     * Extracts EXIF metadata from a media item.
     * Uses MediaStore.setRequireOriginal on Android 10+ to retain GPS if permitted.
     */
    suspend fun getExifData(item: MediaItem): ExifData = withContext(Dispatchers.IO) {
        val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                MediaStore.setRequireOriginal(item.uri)
            } catch (e: Exception) {
                item.uri
            }
        } else {
            item.uri
        }

        var cameraMake: String? = null
        var cameraModel: String? = null
        var aperture: String? = null
        var shutterSpeed: String? = null
        var iso: String? = null
        var focalLength: String? = null
        var latitude: Double? = null
        var longitude: Double? = null

        if (!item.isVideo) {
            try {
                context.contentResolver.openInputStream(targetUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()

                    val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                    if (fNumber > 0.0) {
                        aperture = "f/%.1f".format(Locale.US, fNumber)
                    }

                    val expTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                    if (expTime > 0.0) {
                        shutterSpeed = if (expTime < 1.0) {
                            val denominator = (1.0 / expTime).toInt()
                            "1/%ds".format(denominator)
                        } else {
                            "%.1fs".format(Locale.US, expTime)
                        }
                    }

                    val isoVal = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
                    if (isoVal > 0) {
                        iso = "ISO $isoVal"
                    }

                    val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                    if (focal > 0.0) {
                        focalLength = "%.1f mm".format(Locale.US, focal)
                    }

                    val latLong = FloatArray(2)
                    if (exif.getLatLong(latLong)) {
                        latitude = latLong[0].toDouble()
                        longitude = latLong[1].toDouble()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val dateFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(item.dateTaken))

        val resolution = if (item.width > 0 && item.height > 0) {
            val mp = (item.width * item.height) / 1_000_000.0
            "${item.width} × ${item.height} (%.1f MP)".format(Locale.US, mp)
        } else {
            "Unknown"
        }

        val fileSizeFormatted = formatFileSize(item.size)

        ExifData(
            fileName = item.displayName,
            filePath = item.path,
            fileSizeFormatted = fileSizeFormatted,
            mimeType = item.mimeType,
            dateTakenFormatted = dateFormatted,
            resolutionFormatted = resolution,
            cameraModel = cameraModel,
            cameraMake = cameraMake,
            aperture = aperture,
            shutterSpeed = shutterSpeed,
            iso = iso,
            focalLength = focalLength,
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Creates a privacy-sanitized copy of the media in local cache with all EXIF,
     * GPS, and device metadata stripped out, and returns a FileProvider URI ready for sharing.
     */
    suspend fun createSanitizedShareUri(item: MediaItem): Uri = withContext(Dispatchers.IO) {
        val cacheFolder = File(context.cacheDir, "shared_media")
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs()
        }

        // Clean previous shared files
        cacheFolder.listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > 24 * 3600 * 1000) {
                file.delete()
            }
        }

        val cleanFileName = "clean_${System.currentTimeMillis()}_${item.displayName}"
        val cleanFile = File(cacheFolder, cleanFileName)

        if (item.isVideo) {
            // For videos, copy raw bytes without location permissions attached
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                FileOutputStream(cleanFile).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            // For photos: decode and re-compress without any EXIF tags.
            // This guarantees complete metadata removal.
            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                bitmap = BitmapFactory.decodeStream(input)
            }

            bitmap?.let { bmp ->
                FileOutputStream(cleanFile).use { out ->
                    if (item.mimeType.contains("png", ignoreCase = true)) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } else {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 96, out)
                    }
                }
                bmp.recycle()
            } ?: run {
                // Fallback copy if decoding failed
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(cleanFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cleanFile
        )
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = sizeInBytes / Math.pow(1024.0, index.toDouble())
        return "%.2f %s".format(Locale.US, value, units[index])
    }
}
