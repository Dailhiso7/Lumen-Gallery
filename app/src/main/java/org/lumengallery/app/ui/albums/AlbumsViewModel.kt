package org.lumengallery.app.ui.albums

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lumengallery.app.LumenApplication
import org.lumengallery.app.data.model.MediaAlbum
import org.lumengallery.app.data.model.MediaItem

data class AlbumsUiState(
    val isLoading: Boolean = true,
    val albums: List<MediaAlbum> = emptyList(),
    val albumMedia: List<MediaItem> = emptyList(),
    val columnCount: Int = 3
)

class AlbumsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LumenApplication
    private val repository = app.mediaStoreRepository

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
        observeChanges()
    }

    private fun observeChanges() {
        viewModelScope.launch {
            repository.mediaStoreChangeFlow().collectLatest {
                loadAlbums()
            }
        }
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val albums = repository.queryAlbums()
            _uiState.update { it.copy(isLoading = false, albums = albums) }
        }
    }

    fun loadAlbumMedia(bucketId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val media = repository.queryAllMedia(bucketId = bucketId)
            _uiState.update { it.copy(isLoading = false, albumMedia = media) }
        }
    }

    fun updateColumnCount(newColumns: Int) {
        _uiState.update { it.copy(columnCount = newColumns.coerceIn(2, 5)) }
    }
}
