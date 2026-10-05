package org.lumengallery.app.data.model

data class ExifData(
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val mimeType: String,
    val dateTakenFormatted: String,
    val resolutionFormatted: String,
    val cameraModel: String?,
    val cameraMake: String?,
    val aperture: String?,
    val shutterSpeed: String?,
    val iso: String?,
    val focalLength: String?,
    val latitude: Double?,
    val longitude: Double?
) {
    val hasLocation: Boolean
        get() = latitude != null && longitude != null

    val locationCoordinatesFormatted: String?
        get() = if (hasLocation) {
            "%.5f, %.5f".format(latitude, longitude)
        } else null

    val cameraFullDescription: String?
        get() = when {
            cameraMake != null && cameraModel != null -> {
                if (cameraModel.startsWith(cameraMake, ignoreCase = true)) cameraModel
                else "$cameraMake $cameraModel"
            }
            cameraModel != null -> cameraModel
            cameraMake != null -> cameraMake
            else -> null
        }
}
