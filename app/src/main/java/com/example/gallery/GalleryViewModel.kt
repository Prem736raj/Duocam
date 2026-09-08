package com.example.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class GalleryUiState(
    val recordings: List<RecordingItem> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
)

internal class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecordingRepository(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            val result = runCatching { repository.queryOwnedRecordings() }
            result.onSuccess {
                _uiState.value = GalleryUiState(recordings = it, loading = false)
            }.onFailure {
                _uiState.value = GalleryUiState(
                    loading = false,
                    message = it.message ?: "The local gallery could not be loaded.",
                )
            }
        }
    }

    fun deleteRecording(recording: RecordingItem) {
        viewModelScope.launch {
            val result = runCatching { repository.deleteRecording(recording.uri) }
            result.onSuccess { deleted ->
                if (deleted) {
                    _uiState.value = _uiState.value.copy(
                        recordings = _uiState.value.recordings.filterNot {
                            it.uri == recording.uri
                        },
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        message = "The recording could not be deleted.",
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    message = error.message ?: "The recording could not be deleted.",
                )
            }
        }
    }

    fun shareRecording(recording: RecordingItem) {
        repository.shareRecording(recording).onFailure { error ->
            _uiState.value = _uiState.value.copy(
                message = "Unable to share recording: ${error.message ?: "unknown error"}",
            )
        }
    }

    fun setPermissionDeniedMessage() {
        _uiState.value = GalleryUiState(
            loading = false,
            message = "Storage permission is required to view saved recordings on this Android version.",
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
