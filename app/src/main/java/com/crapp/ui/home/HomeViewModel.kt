package com.crapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.EnergyEntry
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.Location
import com.crapp.data.model.MedicationEntry
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

/** A count of some category of event for a single calendar day, for the frequency charts. */
data class DailyCount(val date: LocalDate, val count: Int)

/**
 * Selectable dashboard time window (docs/future-features.md spec 2) -- replaces the
 * old fixed "last 14 points" / "last 7 days" constants. One shared selection drives
 * every historical chart rather than independent ones, since a single control is
 * simpler to reason about and all of them describe the same recent period.
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
    val energyEntriesToday: Int = 0,
    val walkEntriesToday: Int = 0,
    val lastLoggedAt: Instant? = null,
    val hasAnyEntries: Boolean = false,
    val window: TrendWindow = TrendWindow.FOURTEEN_DAYS,
    val consistencyTrend: List<ConsistencyPoint> = emptyList(),
    val dailyFrequency: List<DailyCount> = emptyList(),
    /** Per-day breakdown of the window totals below -- one bar chart each on the dashboard. */
    val dailyWalk: List<DailyCount> = emptyList(),
    val dailyNight: List<DailyCount> = emptyList(),
    val dailyInsideHome: List<DailyCount> = emptyList(),
    val dailyGarden: List<DailyCount> = emptyList(),
    /**
     * Movements tagged Location.WALK, plus dog-walker-reported [WalkEntry.bowelMovementCount]
     * totals, within the selected window -- spec 3/5. These two sources are deliberately never
     * double-counted at entry time (see the warning on WalkLogScreen), so it's safe to sum them
     * here into one dashboard total.
     */
    val walkMovementsInWindow: Int = 0,
    val nightMovementsInWindow: Int = 0,
    val insideHomeMovementsInWindow: Int = 0,
    val gardenMovementsInWindow: Int = 0
)

/** The four flows besides energy/window -- bundled so the top-level combine stays within kotlinx.coroutines' 5-flow typed overload. */
private data class BaseData(
    val movements: List<BowelMovement>,
    val foodEntries: List<FoodEntry>,
    val medications: List<MedicationEntry>,
    val walkEntries: List<WalkEntry>
)

/**
 * Backs the Dashboard (Home screen, docs/development-plan.md Phase 7): a "today" view
 * up top (today's counts across every logged category) and, scrolling down, the
 * historical picture over a selectable window -- consistency trend, movements per
 * day, and a per-day breakdown of *where* those movements happened (walk / night /
 * inside home / garden), so patterns are visible at a glance without opening History.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CrAppApplication

    private val _window = MutableStateFlow(TrendWindow.FOURTEEN_DAYS)

    private val baseData = combine(
        app.bowelMovementRepository.allMovements,
        app.foodRepository.allFoodEntries,
        app.medicationRepository.allEntries,
        app.walkRepository.allEntries
    ) { movements, foodEntries, medications, walkEntries ->
        BaseData(movements, foodEntries, medications, walkEntries)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseData,
        app.energyRepository.allEntries,
        _window
    ) { base, energyEntries, window ->
        val (movements, foodEntries, medications, walkEntries) = base
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun Instant.toLocalDate() = atZone(zone).toLocalDate()
        fun Instant.isToday() = toLocalDate() == today

        val allTimestamps = listOf(
            movements.map { it.timestamp },
            foodEntries.map { it.timestamp },
            medications.map { it.timestamp },
            energyEntries.map { it.timestamp },
            walkEntries.map { it.timestamp }
        ).flatten()

        // The DAO returns newest-first; the trend chart reads left-to-right
        // chronologically, so re-sort ascending before taking the most recent window.
        val trend = movements
            .sortedBy { it.timestamp }
            .takeLast(window.days)
            .map { ConsistencyPoint(it.timestamp, it.consistency) }

        val windowStart = today.minusDays((window.days - 1).toLong())
        val movementsInWindow = movements.filter { !it.timestamp.toLocalDate().isBefore(windowStart) }
        val walkEntriesInWindow = walkEntries.filter { !it.timestamp.toLocalDate().isBefore(windowStart) }

        fun dailyCounts(countsByDay: Map<LocalDate, Int>): List<DailyCount> =
            (0 until window.days).map { offset ->
                val date = windowStart.plusDays(offset.toLong())
                DailyCount(date, countsByDay[date] ?: 0)
            }

        val overallCountsByDay = movementsInWindow.groupingBy { it.timestamp.toLocalDate() }.eachCount()

        val walkFromMovementsByDay = movementsInWindow
            .filter { it.location == Location.WALK }
            .groupingBy { it.timestamp.toLocalDate() }
            .eachCount()
        val walkFromEntriesByDay = walkEntriesInWindow
            .groupingBy { it.timestamp.toLocalDate() }
            .fold(0) { acc, entry -> acc + entry.bowelMovementCount }
        val walkCountsByDay = (walkFromMovementsByDay.keys + walkFromEntriesByDay.keys).associateWith { date ->
            (walkFromMovementsByDay[date] ?: 0) + (walkFromEntriesByDay[date] ?: 0)
        }

        val nightCountsByDay = movementsInWindow.filter { it.isNightTime }
            .groupingBy { it.timestamp.toLocalDate() }.eachCount()
        val insideHomeCountsByDay = movementsInWindow.filter { it.location == Location.HOME }
            .groupingBy { it.timestamp.toLocalDate() }.eachCount()
        val gardenCountsByDay = movementsInWindow.filter { it.location == Location.GARDEN }
            .groupingBy { it.timestamp.toLocalDate() }.eachCount()

        HomeUiState(
            bowelMovementsToday = movements.count { it.timestamp.isToday() },
            foodEntriesToday = foodEntries.count { it.timestamp.isToday() },
            medicationEntriesToday = medications.count { it.timestamp.isToday() },
            energyEntriesToday = energyEntries.count { it.timestamp.isToday() },
            walkEntriesToday = walkEntries.count { it.timestamp.isToday() },
            lastLoggedAt = allTimestamps.maxOrNull(),
            hasAnyEntries = allTimestamps.isNotEmpty(),
            window = window,
            consistencyTrend = trend,
            dailyFrequency = dailyCounts(overallCountsByDay),
            dailyWalk = dailyCounts(walkCountsByDay),
            dailyNight = dailyCounts(nightCountsByDay),
            dailyInsideHome = dailyCounts(insideHomeCountsByDay),
            dailyGarden = dailyCounts(gardenCountsByDay),
            walkMovementsInWindow = walkCountsByDay.values.sum(),
            nightMovementsInWindow = nightCountsByDay.values.sum(),
            insideHomeMovementsInWindow = insideHomeCountsByDay.values.sum(),
            gardenMovementsInWindow = gardenCountsByDay.values.sum()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onWindowChange(window: TrendWindow) {
        _window.update { window }
    }
}
