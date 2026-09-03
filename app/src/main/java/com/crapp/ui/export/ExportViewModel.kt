package com.crapp.ui.export

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.export.CsvExporter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ExportUiState(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isExporting: Boolean = false,
    val hasAnyData: Boolean = true
)

class ExportViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CrAppApplication
    private val bowelRepo = app.bowelMovementRepository
    private val foodRepo = app.foodRepository
    private val medRepo = app.medicationRepository
    private val energyRepo = app.energyRepository
    private val walkRepo = app.walkRepository
    private val exporter = CsvExporter(application)

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState

    // One-shot channel rather than StateFlow: firing the share sheet must happen
    // exactly once per export, not replay on every recomposition/subscription.
    private val _shareIntent = Channel<Intent>(Channel.BUFFERED)
    val shareIntent: Flow<Intent> = _shareIntent.receiveAsFlow()

    init {
        // Nothing to export yet -- disable the button rather than share three
        // empty (header-only) CSVs.
        viewModelScope.launch {
            combine(
                bowelRepo.allMovements, foodRepo.allFoodEntries, medRepo.allEntries,
                energyRepo.allEntries, walkRepo.allEntries
            ) { m, f, med, energy, walks ->
                m.isNotEmpty() || f.isNotEmpty() || med.isNotEmpty() || energy.isNotEmpty() || walks.isNotEmpty()
            }.collect { hasAnyData -> _uiState.update { it.copy(hasAnyData = hasAnyData) } }
        }
    }

    fun setStartDate(date: LocalDate?) = _uiState.update { it.copy(startDate = date) }

    fun setEndDate(date: LocalDate?) = _uiState.update { it.copy(endDate = date) }

    fun clearDateRange() = _uiState.update { it.copy(startDate = null, endDate = null) }

    fun export() {
        val range = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            val zone = ZoneId.systemDefault()
            val movements = bowelRepo.allMovements.first().filterInRange(range, zone) { it.timestamp }
            val foodEntries = foodRepo.allFoodEntries.first().filterInRange(range, zone) { it.timestamp }
            val foodsById = foodRepo.foodsByRecentUse.first().associateBy { it.id }
            val medications = medRepo.allEntries.first().filterInRange(range, zone) { it.timestamp }
            val energyEntries = energyRepo.allEntries.first().filterInRange(range, zone) { it.timestamp }
            val walkEntries = walkRepo.allEntries.first().filterInRange(range, zone) { it.timestamp }

            val intent = exporter.export(movements, foodEntries, foodsById, medications, energyEntries, walkEntries, zone)
            _uiState.update { it.copy(isExporting = false) }
            _shareIntent.send(intent)
        }
    }

    private inline fun <T> List<T>.filterInRange(
        range: ExportUiState,
        zone: ZoneId,
        timestamp: (T) -> Instant
    ): List<T> = filter {
        val date = timestamp(it).atZone(zone).toLocalDate()
        (range.startDate == null || !date.isBefore(range.startDate)) &&
            (range.endDate == null || !date.isAfter(range.endDate))
    }
}
