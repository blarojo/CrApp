package com.crapp.widget

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.EnergyEntry
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.WalkEntry
import com.crapp.util.countBowelMovementsToday
import com.crapp.util.isToday
import java.time.ZoneId

/**
 * The home screen widget's summary numbers (docs/backlog.md spec 15) -- deliberately
 * a subset of [com.crapp.ui.home.HomeUiState]'s "Today" fields (bowel movements, food,
 * energy only, no medication/walk-report counts), to keep the widget compact per the
 * spec's own "Data model" note. [bowelMovements] itself does fold in dog-walker-
 * reported counts (see [countBowelMovementsToday]) even though a separate "walk"
 * number isn't shown -- those movements genuinely happened today, so leaving them out
 * of the headline count would just be wrong, not "compact". [food]/[energy] are meant
 * to be shown only when non-zero, matching `HomeScreen`'s "Also today: ..."
 * convention -- the widget composable decides that, not this data class.
 */
data class WidgetTodayCounts(
    val bowelMovements: Int = 0,
    val food: Int = 0,
    val energy: Int = 0
)

/**
 * Computes [WidgetTodayCounts] from the same data [com.crapp.ui.home.HomeViewModel]
 * already reads for its own "Today" card, sharing [countBowelMovementsToday]/[isToday]
 * so the two surfaces can't quietly disagree about what counts as "today" or silently
 * under-count a day with a dog-walker-reported walk. A plain function (not a class) so
 * it's trivially unit-testable against fake data with no Android/Glance dependency --
 * see `WidgetTodayCountsTest`.
 */
fun computeWidgetTodayCounts(
    movements: List<BowelMovement>,
    foodEntries: List<FoodEntry>,
    energyEntries: List<EnergyEntry>,
    walkEntries: List<WalkEntry>,
    zone: ZoneId = ZoneId.systemDefault()
): WidgetTodayCounts = WidgetTodayCounts(
    bowelMovements = countBowelMovementsToday(movements, walkEntries, zone),
    food = foodEntries.count { it.timestamp.isToday(zone) },
    energy = energyEntries.count { it.timestamp.isToday(zone) }
)
