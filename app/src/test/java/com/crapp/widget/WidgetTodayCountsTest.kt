package com.crapp.widget

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.EnergyEntry
import com.crapp.data.model.EnergyLevel
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MealType
import com.crapp.data.model.WalkEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/**
 * [computeWidgetTodayCounts] is pure logic (no Android/Glance dependency), so unlike
 * the rest of the widget (see docs/app-functionality.md's Testing status -- Glance's
 * own UI needs a real on-device pass), this part is fully unit-testable.
 */
class WidgetTodayCountsTest {

    private val utc = ZoneOffset.UTC
    private val today = Instant.now()
    private val yesterday = Instant.now().minusSeconds(60 * 60 * 25)

    @Test
    fun computeWidgetTodayCounts_zeroEntries_allZero() {
        val counts = computeWidgetTodayCounts(emptyList(), emptyList(), emptyList(), emptyList(), utc)

        assertEquals(WidgetTodayCounts(0, 0, 0), counts)
    }

    @Test
    fun computeWidgetTodayCounts_onlyBowelMovements_countsThoseAndZeroTheRest() {
        val movements = listOf(
            BowelMovement(timestamp = today, consistency = 4),
            BowelMovement(timestamp = today, consistency = 5),
            BowelMovement(timestamp = yesterday, consistency = 3) // excluded
        )

        val counts = computeWidgetTodayCounts(movements, emptyList(), emptyList(), emptyList(), utc)

        assertEquals(WidgetTodayCounts(bowelMovements = 2, food = 0, energy = 0), counts)
    }

    @Test
    fun computeWidgetTodayCounts_allCategoriesPopulated_countsEachIndependently() {
        val movements = listOf(BowelMovement(timestamp = today, consistency = 4))
        val foodEntries = listOf(
            FoodEntry(timestamp = today, foodId = 1, mealType = MealType.MEAL),
            FoodEntry(timestamp = yesterday, foodId = 1, mealType = MealType.MEAL) // excluded
        )
        val energyEntries = listOf(
            EnergyEntry(timestamp = today, level = EnergyLevel.NORMAL),
            EnergyEntry(timestamp = today, level = EnergyLevel.A_LOT_OF_ENERGY)
        )

        val counts = computeWidgetTodayCounts(movements, foodEntries, energyEntries, emptyList(), utc)

        assertEquals(WidgetTodayCounts(bowelMovements = 1, food = 1, energy = 2), counts)
    }

    @Test
    fun computeWidgetTodayCounts_dogWalkerReportedWalk_foldsIntoBowelMovementsCount() {
        // The exact bug this test guards against: a walk logged today used to leave
        // the widget's headline bowel-movement number undercounted, since only
        // individually-logged BowelMovement rows were counted.
        val movements = listOf(BowelMovement(timestamp = today, consistency = 4))
        val walkEntries = listOf(
            WalkEntry(timestamp = today, bowelMovementCount = 3),
            WalkEntry(timestamp = yesterday, bowelMovementCount = 5) // excluded
        )

        val counts = computeWidgetTodayCounts(movements, emptyList(), emptyList(), walkEntries, utc)

        assertEquals(4, counts.bowelMovements) // 1 logged + 3 from today's walk report
    }
}
