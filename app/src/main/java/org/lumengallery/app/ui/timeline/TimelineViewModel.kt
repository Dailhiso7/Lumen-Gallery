package org.lumengallery.app.ui.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lumengallery.app.LumenApplication
import org.lumengallery.app.data.local.FavoriteEntity
import org.lumengallery.app.data.model.MediaItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TimelineUiState(
    val isLoading: Boolean = true,
    val mediaItems: List<MediaItem> = emptyList(),
    val groupedItems: Map<String, List<MediaItem>> = emptyMap(),
    val columnCount: Int = 3,
    val hasStoragePermission: Boolean = false
)

class TimelineViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LumenApplication
    private val repository = app.mediaStoreRepository
    private val favoriteDao = app.database.favoriteDao()

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        observeMediaStoreChanges()
    }

    fun setPermissionGranted(isGranted: Boolean) {
        _uiState.update { it.copy(hasStoragePermission = isGranted) }
        if (isGranted) {
            loadMedia()
        } else {
            _uiState.update { it.copy(isLoading = false, mediaItems = emptyList(), groupedItems = emptyMap()) }
        }
    }

    private fun observeMediaStoreChanges() {
        viewModelScope.launch {
            repository.mediaStoreChangeFlow().collectLatest {
                if (_uiState.value.hasStoragePermission) {
                    loadMedia()
                }
            }
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = repository.queryAllMedia()
            val grouped = groupItemsByDate(items)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    mediaItems = items,
                    groupedItems = grouped
                )
            }
        }
    }

    fun updateColumnCount(newColumns: Int) {
        val clamped = newColumns.coerceIn(2, 5)
        _uiState.update { it.copy(columnCount = clamped) }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            if (item.isFavorite) {
                favoriteDao.removeFavorite(item.id)
            } else {
                favoriteDao.insertFavorite(FavoriteEntity(item.id, item.uri.toString()))
            }
            loadMedia()
        }
    }

    private fun groupItemsByDate(items: List<MediaItem>): Map<String, List<MediaItem>> {
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(todayCal.time)
        val yesterdayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(yesterdayCal.time)
        val fullDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val checkFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val result = linkedMapOf<String, MutableList<MediaItem>>()

        for (item in items) {
            val itemDate = Date(item.dateTaken)
            val itemKey = checkFormat.format(itemDate)

            val header = when (itemKey) {
                todayDate -> HEADER_TODAY
                yesterdayDate -> HEADER_YESTERDAY
                else -> fullDateFormat.format(itemDate)
            }

            val list = result.getOrPut(header) { mutableListOf() }
            list.add(item)
        }

        return result
    }

    companion object {
        const val HEADER_TODAY = "HEADER_TODAY"
        const val HEADER_YESTERDAY = "HEADER_YESTERDAY"
    }
}
