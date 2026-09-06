package com.crapp.ui.medication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.Medication
import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** Fixed unit choices for [MedicationLogUiState.doseUnit] -- docs/future-features.md's dose/amount spec. */
val MEDICATION_DOSE_UNITS = listOf("mg", "ml", "mcg")

data class MedicationLogUiState(
    val timestamp: Instant = Instant.now(),
    val selectedMedication: Medication? = null,
    val newMedicationName: String = "",
    val dose: String = "",
    val notes: String = "",
    val showAddNewDialog: Boolean = false,
    val isEditing: Boolean = false,
    val saved: Boolean = false,
    /** Structured dose, additive to the free-text [dose] above -- raw text, parsed to Double on save. */
    val doseValueText: String = "",
    val doseUnit: String? = null
)

/**
 * Medication name now comes from a dropdown of the [com.crapp.data.model.Medication]
 * catalog (mirrors [com.crapp.ui.food.FoodLogViewModel]) rather than free text, per
 * the user's request to bring medication logging in line with food logging.
 */
class MedicationLogViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).medicationRepository
    private val editingId: Long = savedStateHandle.get<Long>("id") ?: -1L

    val medicationsByRecentUse: StateFlow<List<Medication>> = repository.medicationsByRecentUse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(MedicationLogUiState(isEditing = editingId != -1L))
    val uiState: StateFlow<MedicationLogUiState> = _uiState.asStateFlow()

    init {
        if (editingId != -1L) {
            viewModelScope.launch {
                repository.getById(editingId)?.let { entry ->
                    // Entries logged before the catalog existed (or restored from a
                    // backup, which deliberately leaves medicationId unset -- see
                    // BackupRepository) have no medicationId yet; lazily link them to
                    // a catalog entry (creating one if needed) the first time they're
                    // opened for edit, same as getOrCreateFood does for foods.
                    val medication = entry.medicationId?.let { repository.getMedicationById(it) }
                        ?: repository.getOrCreateMedication(entry.name).let { repository.getMedicationById(it) }
                    _uiState.update {
                        it.copy(
                            timestamp = entry.timestamp,
                            selectedMedication = medication,
                            dose = entry.dose ?: "",
                            notes = entry.notes ?: "",
                            doseValueText = entry.doseValue?.toString() ?: "",
                            doseUnit = entry.doseUnit
                        )
                    }
                }
            }
        }
    }

    fun onTimestampChange(timestamp: Instant) {
        _uiState.update { it.copy(timestamp = timestamp) }
    }

    fun onMedicationSelected(medication: Medication) {
        _uiState.update { it.copy(selectedMedication = medication) }
    }

    fun onShowAddNewDialog(show: Boolean) {
        _uiState.update { it.copy(showAddNewDialog = show, newMedicationName = "") }
    }

    fun onNewMedicationNameChange(name: String) {
        _uiState.update { it.copy(newMedicationName = name) }
    }

    fun confirmAddNewMedication() {
        val name = _uiState.value.newMedicationName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.getOrCreateMedication(name)
            _uiState.update {
                it.copy(
                    selectedMedication = Medication(id = id, name = name),
                    showAddNewDialog = false,
                    newMedicationName = ""
                )
            }
        }
    }

    fun onDoseChange(dose: String) {
        _uiState.update { it.copy(dose = dose) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onDoseValueTextChange(text: String) {
        _uiState.update { it.copy(doseValueText = text) }
    }

    fun onDoseUnitChange(unit: String) {
        _uiState.update { it.copy(doseUnit = if (it.doseUnit == unit) null else unit) }
    }

    fun save() {
        val state = _uiState.value
        val medication = state.selectedMedication ?: return
        viewModelScope.launch {
            val entry = MedicationEntry(
                id = if (editingId != -1L) editingId else 0,
                timestamp = state.timestamp,
                name = medication.name,
                dose = state.dose.ifBlank { null },
                notes = state.notes.ifBlank { null },
                doseValue = state.doseValueText.toDoubleOrNull(),
                doseUnit = state.doseUnit,
                medicationId = medication.id
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
