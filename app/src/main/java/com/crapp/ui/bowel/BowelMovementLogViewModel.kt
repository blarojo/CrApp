package com.crapp.ui.bowel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val saved: Boolean = false
)

class BowelMovementLogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).bowelMovementRepository

    private val _uiState = MutableStateFlow(BowelMovementLogUiState())
    val uiState: StateFlow<BowelMovementLogUiState> = _uiState.asStateFlow()

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
            repository.add(
                BowelMovement(
                    timestamp = state.timestamp,
                    consistency = state.consistency,
                    hasBlood = state.hasBlood,
                    hasMucus = state.hasMucus,
                    notes = state.notes.ifBlank { null }
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
