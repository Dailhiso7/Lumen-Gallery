package org.lumengallery.app.ui.trash

import android.app.Application
import android.app.PendingIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lumengallery.app.LumenApplication
import org.lumengallery.app.data.model.MediaItem

data class TrashUiState(
    val isLoading: Boolean = true,
    val trashItems: List<MediaItem> = emptyList(),
    val selectedItem: MediaItem? = null
)

class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as LumenApplication).mediaStoreRepository

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadTrash()
    }

    fun loadTrash() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = repository.queryTrashMedia()
            _uiState.update { it.copy(isLoading = false, trashItems = items) }
        }
    }

    fun selectItem(item: MediaItem?) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun restoreItem(item: MediaItem, onPendingIntent: (PendingIntent) -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val pendingIntent = repository.createRestoreFromTrashPendingIntent(listOf(item.uri))
            if (pendingIntent != null) {
                onPendingIntent(pendingIntent)
            } else {
                onSuccess()
            }
        }
    }

    fun deletePermanently(item: MediaItem, onPendingIntent: (PendingIntent) -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val pendingIntent = repository.createDeletePermanentlyPendingIntent(listOf(item.uri))
            if (pendingIntent != null) {
                onPendingIntent(pendingIntent)
            } else {
                val res = repository.deleteDirectly(item.uri)
                if (res.isSuccess) {
                    onSuccess()
                }
            }
        }
    }

    fun emptyTrash(onPendingIntent: (PendingIntent) -> Unit, onSuccess: () -> Unit) {
        val uris = _uiState.value.trashItems.map { it.uri }
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val pendingIntent = repository.createDeletePermanentlyPendingIntent(uris)
            if (pendingIntent != null) {
                onPendingIntent(pendingIntent)
            } else {
                onSuccess()
            }
        }
    }
}
