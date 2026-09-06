package com.crapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crapp.ui.home.ConsistencyPoint
import java.time.Instant
import java.time.LocalDate

/**
 * Recent bowel-movement consistency (Purina 1-7 scale) trend, backed by
 * [ScoreTrendChart] -- see its KDoc for the time-scaled x-axis and tap-to-inspect
 * behavior shared with [EnergyTrendChart].
 *
 * [walkTicks] are dog-walker-reported movements with no real per-movement timestamp
 * (see [com.crapp.ui.home.HomeUiState.walkTicksInWindow]); they're drawn as small
 * unscored tick marks along the bottom axis at their assumed time, distinct from the
 * scored line, so a walk's reported movements are visible on the timeline without
 * fabricating a consistency value for them.
 */
@Composable
fun ConsistencyTrendChart(
    points: List<ConsistencyPoint>,
    windowStart: LocalDate,
    windowDays: Int,
    walkTicks: List<Instant> = emptyList(),
    modifier: Modifier = Modifier
) {
    ScoreTrendChart(
        points = points.map { ScoredPoint(it.timestamp, it.consistency, "consistency ${it.consistency}") },
        windowStart = windowStart,
        windowDays = windowDays,
        minScore = 1,
        maxScore = 7,
        emptyMessage = "Log a bowel movement to see a trend here.",
        extraTicks = walkTicks,
        tickLegend = "Tick marks (｜) are dog-walker-reported movements spread across an assumed hour -- their exact time and consistency aren't known.",
        modifier = modifier
    )
}
