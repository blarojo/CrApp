package com.crapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class TodayTest {

    private val utc: ZoneId = ZoneOffset.UTC

    @Test
    fun isToday_rightNow_true() {
        assertTrue(Instant.now().isToday(utc))
    }

    @Test
    fun isToday_25HoursAgo_false() {
        assertFalse(Instant.now().minusSeconds(60 * 60 * 25).isToday(utc))
    }

    @Test
    fun isToday_25HoursFromNow_false() {
        assertFalse(Instant.now().plusSeconds(60 * 60 * 25).isToday(utc))
    }

    @Test
    fun isToday_defaultsToTheSystemZone() {
        val instant = Instant.now()
        assertEquals(instant.isToday(ZoneId.systemDefault()), instant.isToday())
    }

    @Test
    fun isToday_sameInstantCanDisagreeAcrossZones() {
        // A moment that's already tomorrow in a zone 12 hours ahead is still today
        // (or even yesterday) 24 zone-hours further west -- the same real instant,
        // genuinely different calendar dates, which is exactly why every "today"
        // count in this app must go through one shared zone-aware check rather than
        // each call site picking its own.
        val now = Instant.now()
        val farEast = ZoneOffset.ofHours(14)
        val farWest = ZoneOffset.ofHours(-12)
        assertTrue(now.atZone(farEast).toLocalDate().isAfter(now.atZone(farWest).toLocalDate()))
    }
}
