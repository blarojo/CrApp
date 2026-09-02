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

/** One bowel movement's consistency score, for the trend chart. */
data class ConsistencyPoint(val timestamp: Instant, val consistency: Int)

/** Bowel-movement count for a single calendar day, for the frequency chart. */
data class DailyCount(val date: LocalDate, val count: Int)

data class HomeUiState(
    val bowelMovementsToday: Int = 0,
    val foodEntriesToday: Int = 0,
    val medicationEntriesToday: Int = 0,
    val lastLoggedAt: Instant? = null,
    val hasAnyEntries: Boolean = false,
    val consistencyTrend: List<ConsistencyPoint> = emptyList(),
    val dailyFrequency: List<DailyCount> = emptyList()
)

private const val TREND_POINTS = 14
private const val FREQUENCY_DAYS = 7

/**
 * Backs the Dashboard (Home screen, docs/development-plan.md Phase 7): today's
 * summary (Phase 5) plus two hand-rolled trend charts -- recent consistency scores
 * and movements-per-day over the last week -- so patterns are visible at a glance
 * without opening History.
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
        fun Instant.toLocalDate() = atZone(zone).toLocalDate()
        fun Instant.isToday() = toLocalDate() == today

        val allTimestamps = listOf(
            movements.map { it.timestamp },
            foodEntries.map { it.timestamp },
            medications.map { it.timestamp }
        ).flatten()

        // The DAO returns newest-first; the trend chart reads left-to-right
        // chronologically, so re-sort ascending before taking the most recent window.
        val trend = movements
            .sortedBy { it.timestamp }
            .takeLast(TREND_POINTS)
            .map { ConsistencyPoint(it.timestamp, it.consistency) }

        val windowStart = today.minusDays((FREQUENCY_DAYS - 1).toLong())
        val countsByDay = movements
            .filter { !it.timestamp.toLocalDate().isBefore(windowStart) }
            .groupingBy { it.timestamp.toLocalDate() }
            .eachCount()
        val frequency = (0 until FREQUENCY_DAYS).map { offset ->
            val date = windowStart.plusDays(offset.toLong())
            DailyCount(date, countsByDay[date] ?: 0)
        }

        HomeUiState(
            bowelMovementsToday = movements.count { it.timestamp.isToday() },
            foodEntriesToday = foodEntries.count { it.timestamp.isToday() },
            medicationEntriesToday = medications.count { it.timestamp.isToday() },
            lastLoggedAt = allTimestamps.maxOrNull(),
            hasAnyEntries = allTimestamps.isNotEmpty(),
            consistencyTrend = trend,
            dailyFrequency = frequency
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
