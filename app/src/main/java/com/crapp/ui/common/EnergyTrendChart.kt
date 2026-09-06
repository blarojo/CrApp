package com.crapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crapp.data.model.EnergyLevel
import com.crapp.ui.home.EnergyPoint
import java.time.LocalDate

/**
 * Recent energy-level trend (docs/future-features.md spec 4), backed by the same
 * [ScoreTrendChart] engine as [ConsistencyTrendChart] -- real time-scaled x-axis,
 * one date label per day, tap-to-inspect.
 *
 * [EnergyLevel] is a named 5-point scale, not a number a person would recognize on
 * sight, so the axis plots its `ordinal + 1` (1..5, low-to-high, matching the enum's
 * declared order) but the tap caption shows the level's [EnergyLevel.displayName]
 * (e.g. "Normal") rather than the bare number.
 */
@Composable
fun EnergyTrendChart(
    points: List<EnergyPoint>,
    windowStart: LocalDate,
    windowDays: Int,
    modifier: Modifier = Modifier
) {
    ScoreTrendChart(
        points = points.map { ScoredPoint(it.timestamp, it.level.ordinal + 1, it.level.displayName) },
        windowStart = windowStart,
        windowDays = windowDays,
        minScore = 1,
        maxScore = EnergyLevel.entries.size,
        emptyMessage = "Log an energy level to see a trend here.",
        modifier = modifier
    )
}
