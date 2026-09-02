package com.crapp.ui.medication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val saved: Boolean = false
)

class MedicationLogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).medicationRepository

    private val _uiState = MutableStateFlow(MedicationLogUiState())
    val uiState: StateFlow<MedicationLogUiState> = _uiState.asStateFlow()

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
            repository.add(
                MedicationEntry(
                    timestamp = state.timestamp,
                    name = state.name.trim(),
                    dose = state.dose.ifBlank { null },
                    notes = state.notes.ifBlank { null }
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
