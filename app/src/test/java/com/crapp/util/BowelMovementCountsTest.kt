package com.crapp.util

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.WalkEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/**
 * [countBowelMovementsToday] is the shared fix for a real bug: the "today" bowel-
 * movement count (on both `HomeViewModel`'s own Today card and the home screen
 * widget) used to silently exclude dog-walker-reported [WalkEntry] counts, only
 * counting individually-logged [BowelMovement] rows -- so logging a walk with the
 * dog walker never moved the daily total. These tests guard against that regressing.
 */
class BowelMovementCountsTest {

    private val utc = ZoneOffset.UTC
    private val today = Instant.now()
    private val yesterday = Instant.now().minusSeconds(60 * 60 * 25)

    @Test
    fun countBowelMovementsToday_noEntries_zero() {
        assertEquals(0, countBowelMovementsToday(emptyList(), emptyList(), utc))
    }

    @Test
    fun countBowelMovementsToday_onlyLoggedMovements_countsThem() {
        val movements = listOf(
            BowelMovement(timestamp = today, consistency = 4),
            BowelMovement(timestamp = today, consistency = 6),
            BowelMovement(timestamp = yesterday, consistency = 3) // excluded
        )

        assertEquals(2, countBowelMovementsToday(movements, emptyList(), utc))
    }

    @Test
    fun countBowelMovementsToday_onlyAWalkReport_countsItsBowelMovementCount() {
        val walkEntries = listOf(WalkEntry(timestamp = today, bowelMovementCount = 5))

        assertEquals(5, countBowelMovementsToday(emptyList(), walkEntries, utc))
    }

    @Test
    fun countBowelMovementsToday_loggedMovementsAndAWalkReport_sumsBoth() {
        val movements = listOf(BowelMovement(timestamp = today, consistency = 4))
        val walkEntries = listOf(WalkEntry(timestamp = today, bowelMovementCount = 3))

        assertEquals(4, countBowelMovementsToday(movements, walkEntries, utc))
    }

    @Test
    fun countBowelMovementsToday_yesterdaysWalkReport_excluded() {
        val walkEntries = listOf(WalkEntry(timestamp = yesterday, bowelMovementCount = 9))

        assertEquals(0, countBowelMovementsToday(emptyList(), walkEntries, utc))
    }

    @Test
    fun countBowelMovementsToday_multipleWalkReportsToday_sumsAllOfThem() {
        val walkEntries = listOf(
            WalkEntry(timestamp = today, bowelMovementCount = 2),
            WalkEntry(timestamp = today, bowelMovementCount = 4)
        )

        assertEquals(6, countBowelMovementsToday(emptyList(), walkEntries, utc))
    }
}
