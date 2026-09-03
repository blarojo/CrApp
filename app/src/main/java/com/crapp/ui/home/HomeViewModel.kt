package com.crapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.Location
import com.crapp.data.model.WalkEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One bowel movement's consistency score, for the trend chart. */
data class ConsistencyPoint(val timestamp: Instant, val consistency: Int)

/** Bowel-movement count for a single calendar day, for the frequency chart. */
data class DailyCount(val date: LocalDate, val count: Int)

/**
 * Selectable dashboard time window (docs/future-features.md spec 2) -- replaces the
 * old fixed "last 14 points" / "last 7 days" constants. One shared selection drives
 * both charts rather than two independent ones, since a single control is simpler to
 * reason about and the two charts are meant to describe the same recent period.
 */
enum class TrendWindow(val days: Int, val label: String) {
    SEVEN_DAYS(7, "7d"),
    FOURTEEN_DAYS(14, "14d"),
    THIRTY_DAYS(30, "30d"),
    NINETY_DAYS(90, "90d")
}

data class HomeUiState(
    val bowelMovementsToday: Int = 0,
    val foodEntriesToday: Int = 0,
    val medicationEntriesToday: Int = 0,
    val lastLoggedAt: Instant? = null,
    val hasAnyEntries: Boolean = false,
    val window: TrendWindow = TrendWindow.FOURTEEN_DAYS,
    val consistencyTrend: List<ConsistencyPoint> = emptyList(),
    val dailyFrequency: List<DailyCount> = emptyList(),
    /**
     * Movements tagged Location.WALK, plus dog-walker-reported [WalkEntry.bowelMovementCount]
     * totals, within the selected window -- spec 3/5. These two sources are deliberately never
     * double-counted at entry time (see the warning on WalkLogScreen), so it's safe to sum them
     * here into one dashboard total.
     */
    val walkMovementsInWindow: Int = 0,
    val nightMovementsInWindow: Int = 0
)

/**
 * Backs the Dashboard (Home screen, docs/development-plan.md Phase 7): today's
 * summary (Phase 5) plus two hand-rolled trend charts -- recent consistency scores
 * and movements-per-day over a selectable window (docs/future-features.md spec 2) --
 * so patterns are visible at a glance without opening History. Also rolls up the
 * spec 3 night/walk counts for the same window.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CrAppApplication

    private val _window = MutableStateFlow(TrendWindow.FOURTEEN_DAYS)

    val uiState: StateFlow<HomeUiState> = combine(
        app.bowelMovementRepository.allMovements,
        app.foodRepository.allFoodEntries,
        app.medicationRepository.allEntries,
        app.walkRepository.allEntries,
        _window
    ) { movements, foodEntries, medications, walkEntries, window ->
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
            .takeLast(window.days)
            .map { ConsistencyPoint(it.timestamp, it.consistency) }

        val windowStart = today.minusDays((window.days - 1).toLong())
        val movementsInWindow = movements.filter { !it.timestamp.toLocalDate().isBefore(windowStart) }
        val countsByDay = movementsInWindow
            .groupingBy { it.timestamp.toLocalDate() }
            .eachCount()
        val frequency = (0 until window.days).map { offset ->
            val date = windowStart.plusDays(offset.toLong())
            DailyCount(date, countsByDay[date] ?: 0)
        }

        val walkEntryCountInWindow = walkEntries
            .filter { !it.timestamp.toLocalDate().isBefore(windowStart) }
            .sumOf(WalkEntry::bowelMovementCount)

        HomeUiState(
            bowelMovementsToday = movements.count { it.timestamp.isToday() },
            foodEntriesToday = foodEntries.count { it.timestamp.isToday() },
            medicationEntriesToday = medications.count { it.timestamp.isToday() },
            lastLoggedAt = allTimestamps.maxOrNull(),
            hasAnyEntries = allTimestamps.isNotEmpty(),
            window = window,
            consistencyTrend = trend,
            dailyFrequency = frequency,
            walkMovementsInWindow = movementsInWindow.count { it.location == Location.WALK } + walkEntryCountInWindow,
            nightMovementsInWindow = movementsInWindow.count { it.isNightTime }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onWindowChange(window: TrendWindow) {
        _window.update { window }
    }
}
