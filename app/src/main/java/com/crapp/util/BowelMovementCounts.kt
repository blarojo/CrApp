package com.crapp.util

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.WalkEntry
import java.time.ZoneId

/**
 * Total bowel movements that happened today: every individually-logged
 * [BowelMovement] row, plus every dog-walker-reported [WalkEntry.bowelMovementCount]
 * total. The two are never double-counted at entry time (see the in-app warning on
 * `WalkLogScreen`), so summing them is safe -- the same reasoning `HomeViewModel`'s
 * own "Movements per day" dashboard chart already uses.
 *
 * Shared by [com.crapp.ui.home.HomeViewModel]'s own "Today" hero card and
 * [com.crapp.widget.computeWidgetTodayCounts] so the two can't quietly disagree --
 * before this existed, the "Today" count (both on the dashboard and on the widget)
 * silently excluded walk-reported movements, undercounting any day a walk was only
 * reported as an aggregate count. That bug is exactly why this exists as one shared
 * function rather than each caller reimplementing "count today's movements".
 */
fun countBowelMovementsToday(
    movements: List<BowelMovement>,
    walkEntries: List<WalkEntry>,
    zone: ZoneId = ZoneId.systemDefault()
): Int {
    val fromLoggedMovements = movements.count { it.timestamp.isToday(zone) }
    val fromWalkReports = walkEntries
        .filter { it.timestamp.isToday(zone) }
        .sumOf { it.bowelMovementCount }
    return fromLoggedMovements + fromWalkReports
}
