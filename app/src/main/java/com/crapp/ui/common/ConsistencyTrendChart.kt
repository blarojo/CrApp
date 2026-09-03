package com.crapp.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.crapp.ui.home.ConsistencyPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val axisDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private val tooltipDateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
private const val MIN_SCORE = 1f
private const val MAX_SCORE = 7f

/**
 * Recent bowel-movement consistency (Purina 1-7 scale) as a line + point chart, one
 * series, fixed y-domain so the known clinical scale is always the frame of
 * reference rather than autoscaling to the data. Hand-rolled with Canvas rather than
 * a charting library, matching the project's no-unnecessary-dependencies stance (see
 * docs/development-plan.md Phase 7).
 *
 * Tappable (docs/future-features.md spec 2): a touch device has no hover, so tapping
 * near a point shows its exact date/value in a small caption below the chart instead
 * -- tap the same point again (or elsewhere) to dismiss it.
 */
@Composable
fun ConsistencyTrendChart(
    points: List<ConsistencyPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Text(
            "Log a couple more bowel movements to see a trend here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    val zone = remember { ZoneId.systemDefault() }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = MaterialTheme.colorScheme.secondary
    val chartHeight = 120.dp
    var selectedIndex by remember(points) { mutableIntStateOf(-1) }

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
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            val stepX = if (points.size > 1) size.width.toFloat() / (points.size - 1) else 0f
                            val tappedIndex = if (stepX > 0f) (tapOffset.x / stepX).roundToInt() else 0
                            val clampedIndex = tappedIndex.coerceIn(0, points.size - 1)
                            selectedIndex = if (selectedIndex == clampedIndex) -1 else clampedIndex
                        }
                    }
            ) {
                val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
                fun yFor(score: Int): Float {
                    val fraction = (score - MIN_SCORE) / (MAX_SCORE - MIN_SCORE)
                    return size.height - fraction * size.height
                }

                // Recessive gridlines at each whole point on the 1-7 scale.
                for (level in 1..7) {
                    val y = yFor(level)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }

                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = index * stepX
                    val y = yFor(point.consistency)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                points.forEachIndexed { index, point ->
                    val x = index * stepX
                    val y = yFor(point.consistency)
                    val isSelected = index == selectedIndex
                    drawCircle(
                        color = if (isSelected) selectedColor else lineColor,
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                axisDateFormatter.format(points.first().timestamp.atZone(zone)),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            Text(
                axisDateFormatter.format(points.last().timestamp.atZone(zone)),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
        }
        val selectedPoint = points.getOrNull(selectedIndex)
        if (selectedPoint != null) {
            Text(
                "${tooltipDateFormatter.format(selectedPoint.timestamp.atZone(zone))} — consistency ${selectedPoint.consistency}",
                style = MaterialTheme.typography.labelMedium,
                color = selectedColor,
                modifier = Modifier.padding(top = 4.dp, start = 20.dp)
            )
        }
    }
}
