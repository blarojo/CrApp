package com.crapp.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val axisDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private val tooltipDateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
private val trendDayWidth = 56.dp
private val trendChartHeight = 120.dp
private val tickHeight = 10.dp

/** One timestamped, scored, human-readable-labelled point for [ScoreTrendChart] -- e.g. a consistency score or an energy level. */
data class ScoredPoint(val timestamp: Instant, val score: Int, val label: String)

/**
 * Shared engine behind the dashboard's scored trend charts (consistency, energy):
 * one line + point series on a real time-scaled x-axis, plus optional unscored
 * "tick" markers for events with a known time but no score (e.g. a dog-walker-
 * reported movement). Hand-rolled with Canvas rather than a charting library,
 * matching the project's no-unnecessary-dependencies stance (see
 * docs/development-plan.md Phase 7).
 *
 * The x-axis is a real time scale, not an index: each calendar day in
 * [windowStart, windowStart + windowDays) gets an equal-width column (matching how
 * [FrequencyBarChart] treats days), and a point's position within its day reflects
 * its actual time of day -- two events an hour apart sit close together, not equally
 * spaced regardless of when they happened. Scrolls horizontally with one date label
 * per day, and opens scrolled to the latest day.
 *
 * Tappable (docs/future-features.md spec 2): a touch device has no hover, so tapping
 * near a point shows its exact date/time + [ScoredPoint.label] in a small caption
 * below the chart instead -- tap the same point again (or elsewhere) to dismiss it.
 */
@Composable
fun ScoreTrendChart(
    points: List<ScoredPoint>,
    windowStart: LocalDate,
    windowDays: Int,
    minScore: Int,
    maxScore: Int,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    extraTicks: List<Instant> = emptyList(),
    tickLegend: String? = null
) {
    val zone = remember { ZoneId.systemDefault() }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val dayBoundaryColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = MaterialTheme.colorScheme.secondary
    val tickColor = MaterialTheme.colorScheme.tertiary
    val scrollState = rememberScrollState()
    val sortedPoints = remember(points) { points.sortedBy { it.timestamp } }
    var selectedIndex by remember(points) { mutableIntStateOf(-1) }
    val totalWidth = trendDayWidth * windowDays

    // Scroll to the latest day by default -- a long window otherwise opens on mostly
    // empty early days, hiding the recent data that's usually what's of interest.
    LaunchedEffect(windowStart, windowDays) { scrollState.scrollTo(scrollState.maxValue) }

    // Position in "day units" from windowStart: the integer part is which day, the
    // fractional part is how far through that day (0 = midnight, 1 = next midnight).
    fun dayOffset(instant: Instant): Float {
        val zoned = instant.atZone(zone)
        val daysBetween = ChronoUnit.DAYS.between(windowStart, zoned.toLocalDate()).toFloat()
        val secondsIntoDay = zoned.toLocalTime().toSecondOfDay().toFloat()
        return daysBetween + secondsIntoDay / 86400f
    }

    Column(modifier = modifier) {
        Row {
            Column(
                modifier = Modifier.height(trendChartHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(maxScore.toString(), style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text(minScore.toString(), style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.horizontalScroll(scrollState)) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidth)
                        .height(trendChartHeight)
                        .pointerInput(sortedPoints, windowDays) {
                            detectTapGestures { tapOffset ->
                                if (sortedPoints.isEmpty()) return@detectTapGestures
                                val pxPerDay = size.width.toFloat() / windowDays
                                val tappedOffset = tapOffset.x / pxPerDay
                                val nearestIndex = sortedPoints.indices.minByOrNull { idx ->
                                    abs(dayOffset(sortedPoints[idx].timestamp) - tappedOffset)
                                } ?: 0
                                selectedIndex = if (selectedIndex == nearestIndex) -1 else nearestIndex
                            }
                        }
                ) {
                    val pxPerDay = size.width / windowDays
                    fun xFor(instant: Instant) = dayOffset(instant) * pxPerDay
                    fun yFor(score: Int): Float {
                        val fraction = (score - minScore).toFloat() / (maxScore - minScore)
                        return size.height - fraction * size.height
                    }

                    // Recessive gridlines at each whole point on the score scale.
                    for (level in minScore..maxScore) {
                        val y = yFor(level)
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                    // A vertical line at each day boundary gives the time axis a visible scale.
                    for (day in 0..windowDays) {
                        val x = day * pxPerDay
                        drawLine(dayBoundaryColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                    }

                    if (sortedPoints.size >= 2) {
                        val path = Path()
                        sortedPoints.forEachIndexed { index, point ->
                            val x = xFor(point.timestamp)
                            val y = yFor(point.score)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    sortedPoints.forEachIndexed { index, point ->
                        val x = xFor(point.timestamp)
                        val y = yFor(point.score)
                        val isSelected = index == selectedIndex
                        drawCircle(
                            color = if (isSelected) selectedColor else lineColor,
                            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    extraTicks.forEach { tick ->
                        val x = xFor(tick)
                        drawLine(
                            color = tickColor,
                            start = Offset(x, size.height),
                            end = Offset(x, size.height - tickHeight.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(start = 26.dp)
        ) {
            for (day in 0 until windowDays) {
                Text(
                    axisDateFormatter.format(windowStart.plusDays(day.toLong())),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(trendDayWidth)
                )
            }
        }
        if (points.isEmpty()) {
            Text(
                emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
                modifier = Modifier.padding(top = 4.dp, start = 26.dp)
            )
        } else if (extraTicks.isNotEmpty() && tickLegend != null) {
            Text(
                tickLegend,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.padding(top = 4.dp, start = 26.dp)
            )
        }
        val selectedPoint = sortedPoints.getOrNull(selectedIndex)
        if (selectedPoint != null) {
            Text(
                "${tooltipDateFormatter.format(selectedPoint.timestamp.atZone(zone))} — ${selectedPoint.label}",
                style = MaterialTheme.typography.labelMedium,
                color = selectedColor,
                modifier = Modifier.padding(top = 4.dp, start = 26.dp)
            )
        }
    }
}
