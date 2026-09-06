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
import com.crapp.ui.home.ConsistencyPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val axisDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private val tooltipDateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
private const val MIN_SCORE = 1f
private const val MAX_SCORE = 7f
private val dayWidth = 56.dp
private val chartHeight = 120.dp
private val tickHeight = 10.dp

/**
 * Recent bowel-movement consistency (Purina 1-7 scale) as a line + point chart, one
 * series, fixed y-domain so the known clinical scale is always the frame of
 * reference rather than autoscaling to the data. Hand-rolled with Canvas rather than
 * a charting library, matching the project's no-unnecessary-dependencies stance (see
 * docs/development-plan.md Phase 7).
 *
 * The x-axis is a real time scale, not an index: each calendar day in
 * [windowStart, windowStart + windowDays) gets an equal-width column (matching how
 * [FrequencyBarChart] treats days), and a point's position within its day reflects
 * its actual time of day -- two movements an hour apart sit close together, not
 * equally spaced regardless of when they happened. Scrolls horizontally with one
 * date label per day, rather than showing only the first/last date.
 *
 * [walkTicks] are dog-walker-reported movements with no real per-movement timestamp
 * (see [com.crapp.ui.home.HomeUiState.walkTicksInWindow]); they're drawn as small
 * unscored tick marks along the bottom axis at their assumed time, distinct from the
 * scored line, so a walk's reported movements are visible on the timeline without
 * fabricating a consistency value for them.
 *
 * Tappable (docs/future-features.md spec 2): a touch device has no hover, so tapping
 * near a point shows its exact date/value in a small caption below the chart instead
 * -- tap the same point again (or elsewhere) to dismiss it.
 */
@Composable
fun ConsistencyTrendChart(
    points: List<ConsistencyPoint>,
    windowStart: LocalDate,
    windowDays: Int,
    walkTicks: List<Instant> = emptyList(),
    modifier: Modifier = Modifier
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
    val totalWidth = dayWidth * windowDays

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
                modifier = Modifier.height(chartHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("7", style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text("1", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.horizontalScroll(scrollState)) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidth)
                        .height(chartHeight)
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
                        val fraction = (score - MIN_SCORE) / (MAX_SCORE - MIN_SCORE)
                        return size.height - fraction * size.height
                    }

                    // Recessive gridlines at each whole point on the 1-7 scale.
                    for (level in 1..7) {
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
                            val y = yFor(point.consistency)
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
                        val y = yFor(point.consistency)
                        val isSelected = index == selectedIndex
                        drawCircle(
                            color = if (isSelected) selectedColor else lineColor,
                            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    walkTicks.forEach { tick ->
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
                    modifier = Modifier.width(dayWidth)
                )
            }
        }
        if (points.isEmpty()) {
            Text(
                "Log a bowel movement to see a trend here.",
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
                modifier = Modifier.padding(top = 4.dp, start = 26.dp)
            )
        } else if (walkTicks.isNotEmpty()) {
            Text(
                "Tick marks (｜) are dog-walker-reported movements spread across an assumed hour -- their exact time and consistency aren't known.",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.padding(top = 4.dp, start = 26.dp)
            )
        }
        val selectedPoint = sortedPoints.getOrNull(selectedIndex)
        if (selectedPoint != null) {
            Text(
                "${tooltipDateFormatter.format(selectedPoint.timestamp.atZone(zone))} — consistency ${selectedPoint.consistency}",
                style = MaterialTheme.typography.labelMedium,
                color = selectedColor,
                modifier = Modifier.padding(top = 4.dp, start = 26.dp)
            )
        }
    }
}
