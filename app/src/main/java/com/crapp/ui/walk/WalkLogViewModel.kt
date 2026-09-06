package com.crapp.ui.walk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.WalkEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class WalkLogUiState(
    val timestamp: Instant = Instant.now(),
    val bowelMovementCount: Int = 0,
    val notes: String = "",
    val isEditing: Boolean = false,
    val saved: Boolean = false
)

/**
 * Backs the dog walker's walk-report screen (docs/future-features.md spec 5): a
 * count only, no per-movement detail. Deliberately separate from
 * [com.crapp.ui.bowel.BowelMovementLogScreen] -- see [WalkEntry]'s KDoc for why both
 * exist, and the screen's inline warning for avoiding double-counting.
 */
class WalkLogViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).walkRepository
    private val editingId: Long = savedStateHandle.get<Long>("id") ?: -1L

    private val _uiState = MutableStateFlow(WalkLogUiState(isEditing = editingId != -1L))
    val uiState: StateFlow<WalkLogUiState> = _uiState.asStateFlow()

    init {
        if (editingId != -1L) {
            viewModelScope.launch {
                repository.getById(editingId)?.let { entry ->
                    _uiState.update {
                        it.copy(
                            timestamp = entry.timestamp,
                            bowelMovementCount = entry.bowelMovementCount,
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

    fun onCountChange(count: Int) {
        _uiState.update { it.copy(bowelMovementCount = count.coerceAtLeast(0)) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            val entry = WalkEntry(
                id = if (editingId != -1L) editingId else 0,
                timestamp = state.timestamp,
                bowelMovementCount = state.bowelMovementCount,
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
