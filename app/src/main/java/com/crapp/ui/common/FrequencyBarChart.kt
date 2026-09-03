package com.crapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.crapp.ui.home.DailyCount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE")
private val fullDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val barColumnWidth = 28.dp
private val chartHeight = 96.dp

/**
 * Bowel movements per day over a selectable recent window (docs/future-features.md
 * spec 2 -- was a fixed last-7-days; [days] is zero-filled for days with no entries
 * so gaps are visible rather than just missing bars). One series, single hue, per
 * docs/development-plan.md Phase 7.
 *
 * Scrolls horizontally rather than squeezing bars narrower for a longer window
 * (e.g. 90 days), and tapping a bar/label shows its full date -- the day-of-week
 * label alone is ambiguous once a window spans more than a single week.
 */
@Composable
fun FrequencyBarChart(
    days: List<DailyCount>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val barColor = MaterialTheme.colorScheme.primary
    val emptyBarColor = barColor.copy(alpha = 0.15f)
    val selectedColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = remember(days) { (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1) }
    var selectedDate by remember(days) { mutableStateOf<LocalDate?>(null) }
    val scrollState = rememberScrollState()

    fun toggle(date: LocalDate) {
        selectedDate = if (selectedDate == date) null else date
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            days.forEach { day ->
                val barHeight = (chartHeight * (day.count.toFloat() / maxCount)).coerceAtLeast(3.dp)
                val isSelected = day.date == selectedDate
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .width(barColumnWidth)
                        .height(chartHeight + 20.dp)
                        .clickable { toggle(day.date) }
                ) {
                    if (day.count > 0) {
                        Text(day.count.toString(), style = MaterialTheme.typography.labelSmall, color = labelColor)
                    }
                    Box(
                        modifier = Modifier
                            .width(barColumnWidth)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                when {
                                    isSelected -> selectedColor
                                    day.count > 0 -> barColor
                                    else -> emptyBarColor
                                }
                            )
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            days.forEach { day ->
                Text(
                    dayOfWeekFormatter.format(day.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.date == selectedDate) selectedColor else labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(barColumnWidth)
                        .clickable { toggle(day.date) }
                )
            }
        }
        val selected = selectedDate
        if (selected != null) {
            val count = days.firstOrNull { it.date == selected }?.count ?: 0
            Text(
                "${fullDateFormatter.format(selected)} — $count movement${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = selectedColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
