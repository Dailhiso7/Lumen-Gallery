package org.lumengallery.app.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lumengallery.app.R
import org.lumengallery.app.data.model.ExifData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifBottomSheet(
    exifData: ExifData?,
    isLoading: Boolean,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.exif_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading || exifData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                // File info
                ExifItemRow(
                    icon = Icons.Default.Info,
                    title = exifData.fileName,
                    subtitle = "${exifData.fileSizeFormatted} • ${exifData.mimeType}"
                )

                // Date taken
                ExifItemRow(
                    icon = Icons.Default.DateRange,
                    title = stringResource(R.string.exif_date),
                    subtitle = exifData.dateTakenFormatted
                )

                // Resolution
                ExifItemRow(
                    icon = Icons.Default.PhotoSizeSelectActual,
                    title = stringResource(R.string.exif_resolution),
                    subtitle = exifData.resolutionFormatted
                )

                // Camera Details
                if (exifData.cameraFullDescription != null) {
                    ExifItemRow(
                        icon = Icons.Default.CameraAlt,
                        title = stringResource(R.string.exif_camera),
                        subtitle = exifData.cameraFullDescription ?: "Unknown"
                    )
                }

                // Exposure Parameters
                val hasParams = exifData.aperture != null || exifData.shutterSpeed != null ||
                        exifData.iso != null || exifData.focalLength != null

                if (hasParams) {
                    val paramsText = listOfNotNull(
                        exifData.aperture,
                        exifData.shutterSpeed,
                        exifData.iso,
                        exifData.focalLength
                    ).joinToString("   •   ")

                    ExifItemRow(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.exif_parameters),
                        subtitle = paramsText
                    )
                }

                // Location / Privacy
                if (exifData.hasLocation) {
                    ExifItemRow(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.exif_location),
                        subtitle = exifData.locationCoordinatesFormatted ?: ""
                    )
                } else {
                    ExifItemRow(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.exif_location),
                        subtitle = stringResource(R.string.exif_no_location)
                    )
                }

                // File path
                if (exifData.filePath.isNotBlank()) {
                    ExifItemRow(
                        icon = Icons.Default.Folder,
                        title = stringResource(R.string.exif_file_path),
                        subtitle = exifData.filePath
                    )
                }
            }
        }
    }
}

@Composable
private fun ExifItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
