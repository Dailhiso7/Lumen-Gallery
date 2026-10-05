package org.lumengallery.app.ui.viewer

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.lumengallery.app.ui.components.ZoomableBox

@Composable
fun MediaViewerScreen(
    mediaId: Long,
    bucketId: Long = -1L,
    isFavorites: Boolean = false,
    viewModel: MediaViewerViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showExifSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(mediaId) {
        viewModel.initialize(mediaId, bucketId, isFavorites)
    }

    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onBackClick()
        }
    }

    val renameIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Reload
            state.currentItem?.let { viewModel.initialize(it.id, bucketId, isFavorites) }
        }
    }

    if (state.items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.currentIndex.coerceIn(0, (state.items.size - 1).coerceAtLeast(0)),
        pageCount = { state.items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    val currentItem = state.currentItem

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Horizontal Pager for swiping media
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { state.items[it].id }
        ) { page ->
            val item = state.items[page]
            if (item.isVideo) {
                VideoPlayer(
                    uri = item.uri,
                    isControlsVisible = showControls,
                    onToggleControls = { showControls = !showControls },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ZoomableBox(
                    maxScale = 4.0f,
                    onDismiss = onBackClick,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Top Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentItem?.displayName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${state.items.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(onClick = { showExifSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "EXIF Details",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }

                    // Favorite
                    IconButton(
                        onClick = {
                            currentItem?.let { viewModel.toggleFavorite(it) }
                        }
                    ) {
                        Icon(
                            imageVector = if (currentItem?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentItem?.isFavorite == true) Color(0xFFFF5252) else Color.White
                        )
                    }

                    // Rename
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = Color.White
                        )
                    }

                    // Delete
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Loading indicator for privacy sanitizing share
        if (state.isSharingClean) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Stripping metadata...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    // EXIF Details Bottom Sheet
    if (showExifSheet) {
        ExifBottomSheet(
            exifData = state.currentExif,
            isLoading = state.isExifLoading,
            onDismissRequest = { showExifSheet = false }
        )
    }

    // Share Options Dialog (Direct vs Clean Share)
    if (showShareDialog && currentItem != null) {
        ShareOptionsDialog(
            onDismissRequest = { showShareDialog = false },
            onDirectShare = {
                showShareDialog = false
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = currentItem.mimeType
                    putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share media"))
            },
            onCleanShare = {
                showShareDialog = false
                viewModel.createSanitizedShareUri(currentItem) { cleanUri ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = currentItem.mimeType
                        putExtra(Intent.EXTRA_STREAM, cleanUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share without metadata"))
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && currentItem != null) {
        RenameDialog(
            currentName = currentItem.displayName,
            onDismissRequest = { showRenameDialog = false },
            onConfirm = { newName ->
                showRenameDialog = false
                viewModel.renameCurrentItem(
                    newName = newName,
                    onPendingIntent = { pendingIntent ->
                        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        renameIntentLauncher.launch(request)
                    },
                    onSuccess = {
                        viewModel.initialize(currentItem.id, bucketId, isFavorites)
                    }
                )
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && currentItem != null) {
        DeleteConfirmDialog(
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteCurrentItem(
                    onPendingIntent = { pendingIntent ->
                        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        deleteIntentLauncher.launch(request)
                    },
                    onSuccess = {
                        onBackClick()
                    }
                )
            }
        )
    }
}
