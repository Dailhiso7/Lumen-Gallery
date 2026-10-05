package org.lumengallery.app.ui.viewer

import android.app.Application
import android.app.PendingIntent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lumengallery.app.LumenApplication
import org.lumengallery.app.data.local.FavoriteEntity
import org.lumengallery.app.data.model.ExifData
import org.lumengallery.app.data.model.MediaItem

data class ViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentExif: ExifData? = null,
    val isExifLoading: Boolean = false,
    val isSharingClean: Boolean = false
) {
    val currentItem: MediaItem?
        get() = items.getOrNull(currentIndex)
}

class MediaViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LumenApplication
    private val repository = app.mediaStoreRepository
    private val favoriteDao = app.database.favoriteDao()
    private val exifManager = app.exifMetadataManager

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    fun initialize(mediaId: Long, bucketId: Long = -1L, isFavorites: Boolean = false) {
        viewModelScope.launch {
            val allMedia = when {
                bucketId > 0 -> repository.queryAllMedia(bucketId)
                isFavorites -> repository.queryAllMedia().filter { it.isFavorite }
                else -> repository.queryAllMedia()
            }

            val targetIndex = allMedia.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    items = allMedia,
                    currentIndex = targetIndex
                )
            }

            allMedia.getOrNull(targetIndex)?.let { loadExif(it) }
        }
    }

    fun onPageChanged(newIndex: Int) {
        if (newIndex != _uiState.value.currentIndex && newIndex in _uiState.value.items.indices) {
            _uiState.update { it.copy(currentIndex = newIndex, currentExif = null) }
            val item = _uiState.value.items[newIndex]
            loadExif(item)
        }
    }

    private fun loadExif(item: MediaItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExifLoading = true) }
            val exif = exifManager.getExifData(item)
            _uiState.update { it.copy(currentExif = exif, isExifLoading = false) }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val isNowFavorite = !item.isFavorite
            if (isNowFavorite) {
                favoriteDao.insertFavorite(FavoriteEntity(item.id, item.uri.toString()))
            } else {
                favoriteDao.removeFavorite(item.id)
            }

            // Update item in local list
            _uiState.update { state ->
                val updatedList = state.items.map {
                    if (it.id == item.id) it.copy(isFavorite = isNowFavorite) else it
                }
                state.copy(items = updatedList)
            }
        }
    }

    fun createSanitizedShareUri(item: MediaItem, onReady: (Uri) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSharingClean = true) }
            val uri = exifManager.createSanitizedShareUri(item)
            _uiState.update { it.copy(isSharingClean = false) }
            onReady(uri)
        }
    }

    fun deleteCurrentItem(onPendingIntent: (PendingIntent) -> Unit, onSuccess: () -> Unit) {
        val current = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            val pendingIntent = repository.createDeletePendingIntent(listOf(current.uri))
            if (pendingIntent != null) {
                onPendingIntent(pendingIntent)
            } else {
                val result = repository.deleteDirectly(current.uri)
                if (result.isSuccess) {
                    onSuccess()
                }
            }
        }
    }

    fun renameCurrentItem(
        newName: String,
        onPendingIntent: (PendingIntent) -> Unit,
        onSuccess: () -> Unit
    ) {
        val current = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            val result = repository.renameMedia(current.uri, newName)
            result.onSuccess { pendingIntent ->
                if (pendingIntent != null) {
                    onPendingIntent(pendingIntent)
                } else {
                    onSuccess()
                }
            }
        }
    }
}
