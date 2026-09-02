package com.crapp.ui.medication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class MedicationLogUiState(
    val timestamp: Instant = Instant.now(),
    val name: String = "",
    val dose: String = "",
    val notes: String = "",
    val isEditing: Boolean = false,
    val saved: Boolean = false
)

class MedicationLogViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).medicationRepository
    private val editingId: Long = savedStateHandle.get<Long>("id") ?: -1L

    private val _uiState = MutableStateFlow(MedicationLogUiState(isEditing = editingId != -1L))
    val uiState: StateFlow<MedicationLogUiState> = _uiState.asStateFlow()

    init {
        if (editingId != -1L) {
            viewModelScope.launch {
                repository.getById(editingId)?.let { entry ->
                    _uiState.update {
                        it.copy(
                            timestamp = entry.timestamp,
                            name = entry.name,
                            dose = entry.dose ?: "",
                            notes = entry.notes ?: ""
                        )
                    }
                }
            }
        }
    }

    fun onTimestampChange(timestamp: Instant) {
        _uiState.update { it.copy(timestamp = timestamp) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onDoseChange(dose: String) {
        _uiState.update { it.copy(dose = dose) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            val entry = MedicationEntry(
                id = if (editingId != -1L) editingId else 0,
                timestamp = state.timestamp,
                name = state.name.trim(),
                dose = state.dose.ifBlank { null },
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
