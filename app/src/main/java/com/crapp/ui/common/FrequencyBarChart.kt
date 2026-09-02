package com.crapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.crapp.ui.home.DailyCount
import java.time.format.DateTimeFormatter

private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE")
private val barColumnWidth = 28.dp
private val chartHeight = 96.dp

/**
 * Bowel movements per day over a fixed recent window (e.g. last 7 days, zero-filled
 * for days with no entries) so gaps are visible rather than just missing bars. One
 * series, single hue, per docs/development-plan.md Phase 7.
 */
@Composable
fun FrequencyBarChart(
    days: List<DailyCount>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val barColor = MaterialTheme.colorScheme.primary
    val emptyBarColor = barColor.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = remember(days) { (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                val barHeight = (chartHeight * (day.count.toFloat() / maxCount)).coerceAtLeast(3.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.height(chartHeight + 20.dp)
                ) {
                    if (day.count > 0) {
                        Text(day.count.toString(), style = MaterialTheme.typography.labelSmall, color = labelColor)
                    }
                    Box(
                        modifier = Modifier
                            .width(barColumnWidth)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (day.count > 0) barColor else emptyBarColor)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                Text(
                    dayOfWeekFormatter.format(day.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(barColumnWidth)
                )
            }
        }
    }
}
