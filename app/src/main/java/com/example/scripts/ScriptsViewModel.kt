package com.example.scripts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.Script
import com.example.db.ScriptDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ScriptsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScriptRepository(
        ScriptDatabase.getDatabase(application).scriptDao(),
    )

    val scripts: StateFlow<List<Script>> = repository.scripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    fun clearError() {
        _operationError.value = null
    }

    fun save(existing: Script?, title: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.save(existing, title, content) }
            }
            result.onSuccess { onSuccess() }
                .onFailure {
                    _operationError.value = it.message ?: "The script could not be saved."
                }
        }
    }

    fun delete(script: Script) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.delete(script) }
            }
            result.onFailure {
                _operationError.value = it.message ?: "The script could not be deleted."
            }
        }
    }
}
