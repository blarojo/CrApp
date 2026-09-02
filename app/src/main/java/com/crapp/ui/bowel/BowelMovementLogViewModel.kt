package com.crapp.ui.bowel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.BowelMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class BowelMovementLogUiState(
    val timestamp: Instant = Instant.now(),
    val consistency: Int = 4,
    val hasBlood: Boolean = false,
    val hasMucus: Boolean = false,
    val notes: String = "",
    val isEditing: Boolean = false,
    val saved: Boolean = false
)

class BowelMovementLogViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).bowelMovementRepository
    private val editingId: Long = savedStateHandle.get<Long>("id") ?: -1L

    private val _uiState = MutableStateFlow(BowelMovementLogUiState(isEditing = editingId != -1L))
    val uiState: StateFlow<BowelMovementLogUiState> = _uiState.asStateFlow()

    init {
        if (editingId != -1L) {
            viewModelScope.launch {
                repository.getById(editingId)?.let { movement ->
                    _uiState.update {
                        it.copy(
                            timestamp = movement.timestamp,
                            consistency = movement.consistency,
                            hasBlood = movement.hasBlood,
                            hasMucus = movement.hasMucus,
                            notes = movement.notes ?: ""
                        )
                    }
                }
            }
        }
    }

    fun onTimestampChange(timestamp: Instant) {
        _uiState.update { it.copy(timestamp = timestamp) }
    }

    fun onConsistencyChange(consistency: Int) {
        _uiState.update { it.copy(consistency = consistency) }
    }

    fun onHasBloodChange(hasBlood: Boolean) {
        _uiState.update { it.copy(hasBlood = hasBlood) }
    }

    fun onHasMucusChange(hasMucus: Boolean) {
        _uiState.update { it.copy(hasMucus = hasMucus) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            val movement = BowelMovement(
                id = if (editingId != -1L) editingId else 0,
                timestamp = state.timestamp,
                consistency = state.consistency,
                hasBlood = state.hasBlood,
                hasMucus = state.hasMucus,
                notes = state.notes.ifBlank { null }
            )
            if (editingId != -1L) repository.update(movement) else repository.add(movement)
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
