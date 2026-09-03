package com.crapp.ui.medicationcatalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.Medication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Medication Catalog admin screen -- mirrors `FoodCatalogViewModel`. */
class MedicationCatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).medicationRepository

    val medications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _pendingDelete = MutableStateFlow<Medication?>(null)
    val pendingDelete: StateFlow<Medication?> = _pendingDelete.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun cancelAddDialog() {
        _showAddDialog.value = false
    }

    fun addMedication(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.getOrCreateMedication(trimmed)
            _showAddDialog.value = false
        }
    }

    fun requestDelete(medication: Medication) {
        _pendingDelete.value = medication
    }

    fun cancelDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val medication = _pendingDelete.value ?: return
        viewModelScope.launch {
            repository.deleteMedication(medication)
            _message.value = "Deleted \"${medication.name}\"."
            _pendingDelete.value = null
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
