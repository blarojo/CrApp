package com.crapp.ui.energy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.EnergyEntry
import com.crapp.data.model.EnergyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class EnergyLogUiState(
    val timestamp: Instant = Instant.now(),
    val level: EnergyLevel = EnergyLevel.NORMAL,
    val notes: String = "",
    val isEditing: Boolean = false,
    val saved: Boolean = false
)

class EnergyLogViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).energyRepository
    private val editingId: Long = savedStateHandle.get<Long>("id") ?: -1L

    private val _uiState = MutableStateFlow(EnergyLogUiState(isEditing = editingId != -1L))
    val uiState: StateFlow<EnergyLogUiState> = _uiState.asStateFlow()

    init {
        if (editingId != -1L) {
            viewModelScope.launch {
                repository.getById(editingId)?.let { entry ->
                    _uiState.update {
                        it.copy(timestamp = entry.timestamp, level = entry.level, notes = entry.notes ?: "")
                    }
                }
            }
        }
    }

    fun onTimestampChange(timestamp: Instant) {
        _uiState.update { it.copy(timestamp = timestamp) }
    }

    fun onLevelChange(level: EnergyLevel) {
        _uiState.update { it.copy(level = level) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            val entry = EnergyEntry(
                id = if (editingId != -1L) editingId else 0,
                timestamp = state.timestamp,
                level = state.level,
                notes = state.notes.ifBlank { null }
            )
            if (editingId != -1L) repository.update(entry) else repository.add(entry)
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (editingId == -1L) return
        viewModelScope.launch {
            repository.getById(editingId)?.let { repository.delete(it) }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
