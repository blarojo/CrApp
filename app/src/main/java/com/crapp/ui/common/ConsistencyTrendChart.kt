package com.crapp.ui.common

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.crapp.ui.home.ConsistencyPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val axisDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private const val MIN_SCORE = 1f
private const val MAX_SCORE = 7f

/**
 * Recent bowel-movement consistency (Purina 1-7 scale) as a line + point chart, one
 * series, fixed y-domain so the known clinical scale is always the frame of
 * reference rather than autoscaling to the data. Hand-rolled with Canvas rather than
 * a charting library, matching the project's no-unnecessary-dependencies stance (see
 * docs/development-plan.md Phase 7).
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
    val chartHeight = 120.dp

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
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
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
    }
}
