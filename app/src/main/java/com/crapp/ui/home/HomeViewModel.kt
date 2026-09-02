package com.crapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val bowelMovementsToday: Int = 0,
    val foodEntriesToday: Int = 0,
    val medicationEntriesToday: Int = 0,
    val lastLoggedAt: Instant? = null,
    val hasAnyEntries: Boolean = false
)

/**
 * Backs the Home screen's "today" summary (see docs/development-plan.md Phase 5):
 * today's bowel-movement count and the most recent entry of any type, so there's a
 * quick at-a-glance sense of what's already been logged without opening History.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CrAppApplication

    val uiState: StateFlow<HomeUiState> = combine(
        app.bowelMovementRepository.allMovements,
        app.foodRepository.allFoodEntries,
        app.medicationRepository.allEntries
    ) { movements, foodEntries, medications ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun Instant.isToday() = atZone(zone).toLocalDate() == today

        val allTimestamps = listOf(
            movements.map { it.timestamp },
            foodEntries.map { it.timestamp },
            medications.map { it.timestamp }
        ).flatten()

        HomeUiState(
            bowelMovementsToday = movements.count { it.timestamp.isToday() },
            foodEntriesToday = foodEntries.count { it.timestamp.isToday() },
            medicationEntriesToday = medications.count { it.timestamp.isToday() },
            lastLoggedAt = allTimestamps.maxOrNull(),
            hasAnyEntries = allTimestamps.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
