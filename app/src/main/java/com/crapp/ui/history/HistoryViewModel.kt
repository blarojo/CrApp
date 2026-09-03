package com.crapp.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HistoryFilter(
    val types: Set<HistoryEntryType> = HistoryEntryType.entries.toSet(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

data class HistoryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val filter: HistoryFilter = HistoryFilter(),
    val pendingDelete: HistoryEntry? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CrAppApplication
    private val bowelRepo = app.bowelMovementRepository
    private val foodRepo = app.foodRepository
    private val medRepo = app.medicationRepository
    private val energyRepo = app.energyRepository
    private val walkRepo = app.walkRepository

    // kotlinx.coroutines' combine() tops out at 5 typed flows, so this nests two
    // combine() calls rather than casting through a vararg Array<Any?> overload.
    private val bowelFoodMedHistory: StateFlow<List<HistoryEntry>> = combine(
        bowelRepo.allMovements, foodRepo.allFoodEntries, foodRepo.foodsByRecentUse, medRepo.allEntries
    ) { movements, foodEntries, foods, medications ->
        val foodsById = foods.associateBy { it.id }
        movements.map { HistoryEntry.BowelMovementEntry(it) } +
            foodEntries.map { HistoryEntry.FoodLogEntry(it, foodsById[it.foodId]) } +
            medications.map { HistoryEntry.MedicationLogEntry(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allEntries: StateFlow<List<HistoryEntry>> = combine(
        bowelFoodMedHistory, energyRepo.allEntries, walkRepo.allEntries
    ) { partial, energyEntries, walkEntries ->
        (partial + energyEntries.map { HistoryEntry.EnergyLogEntry(it) } + walkEntries.map { HistoryEntry.WalkLogEntry(it) })
            .sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(HistoryFilter())
    private val _pendingDelete = MutableStateFlow<HistoryEntry?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        allEntries, _filter, _pendingDelete
    ) { entries, filter, pendingDelete ->
        val zone = ZoneId.systemDefault()
        val filtered = entries.filter { entry ->
            if (entry.type !in filter.types) return@filter false
            val date = entry.timestamp.atZone(zone).toLocalDate()
            if (filter.startDate != null && date.isBefore(filter.startDate)) return@filter false
            if (filter.endDate != null && date.isAfter(filter.endDate)) return@filter false
            true
        }
        HistoryUiState(entries = filtered, filter = filter, pendingDelete = pendingDelete)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun toggleType(type: HistoryEntryType) {
        _filter.update { current ->
            val newTypes = if (type in current.types) current.types - type else current.types + type
            current.copy(types = newTypes)
        }
    }

    fun setStartDate(date: LocalDate?) {
        _filter.update { it.copy(startDate = date) }
    }

    fun setEndDate(date: LocalDate?) {
        _filter.update { it.copy(endDate = date) }
    }

    fun clearDateRange() {
        _filter.update { it.copy(startDate = null, endDate = null) }
    }

    fun requestDelete(entry: HistoryEntry) {
        _pendingDelete.value = entry
    }

    fun cancelDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val entry = _pendingDelete.value ?: return
        viewModelScope.launch {
            when (entry) {
                is HistoryEntry.BowelMovementEntry -> bowelRepo.delete(entry.movement)
                is HistoryEntry.FoodLogEntry -> foodRepo.deleteFoodEntry(entry.entry)
                is HistoryEntry.MedicationLogEntry -> medRepo.delete(entry.entry)
                is HistoryEntry.EnergyLogEntry -> energyRepo.delete(entry.entry)
                is HistoryEntry.WalkLogEntry -> walkRepo.delete(entry.entry)
            }
            _pendingDelete.value = null
        }
    }
}
