package com.crapp.ui.gallery

import com.crapp.data.model.Amount
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * [buildGalleryCaption] is the pure "what shows, what's omitted" logic behind every
 * Gallery card and the full-size viewer (docs/backlog.md spec 14). These tests guard
 * the omit-an-empty-line rules specifically, since those are easy to get subtly wrong.
 */
class GalleryCaptionTest {

    private val utc = ZoneOffset.UTC
    private val timestamp = LocalDateTime.of(2026, 9, 9, 14, 30).toInstant(utc)

    @Test
    fun buildGalleryCaption_minimalMovement_onlyConsistencyLineShown() {
        val movement = BowelMovement(timestamp = timestamp, consistency = 4)

        val caption = buildGalleryCaption(movement, utc)

        // Uses the production formatter itself rather than a hardcoded string, so
        // this doesn't depend on the test JVM's default locale (e.g. "Sep" vs "Sept",
        // "PM" vs "pm") -- only that buildGalleryCaption formats the timestamp the
        // same way HistoryScreen's identical-pattern formatter does.
        assertEquals(galleryDateTimeFormatter.format(timestamp.atZone(utc)), caption.dateTime)
        assertEquals("Consistency 4", caption.consistencyAndAmount)
        assertNull(caption.locationAndNight)
    }

    @Test
    fun buildGalleryCaption_withAmount_appendsToConsistencyLine() {
        val movement = BowelMovement(timestamp = timestamp, consistency = 5, amount = Amount.MEDIUM_AMOUNT)

        val caption = buildGalleryCaption(movement, utc)

        assertEquals("Consistency 5 · Medium amount", caption.consistencyAndAmount)
    }

    @Test
    fun buildGalleryCaption_withLocation_showsDisplayName() {
        val movement = BowelMovement(timestamp = timestamp, consistency = 4, location = Location.GARDEN)

        val caption = buildGalleryCaption(movement, utc)

        assertEquals("Garden", caption.locationAndNight)
    }

    @Test
    fun buildGalleryCaption_otherLocationWithText_showsOtherPrefixed() {
        val movement = BowelMovement(
            timestamp = timestamp,
            consistency = 4,
            location = Location.OTHER,
            locationOther = "Vet clinic"
        )

        val caption = buildGalleryCaption(movement, utc)

        assertEquals("Other: Vet clinic", caption.locationAndNight)
    }

    @Test
    fun buildGalleryCaption_otherLocationNoText_showsPlainOther() {
        val movement = BowelMovement(timestamp = timestamp, consistency = 4, location = Location.OTHER)

        val caption = buildGalleryCaption(movement, utc)

        assertEquals("Other", caption.locationAndNight)
    }

    @Test
    fun buildGalleryCaption_nightOnly_showsNightWithNoLocation() {
        val movement = BowelMovement(timestamp = timestamp, consistency = 4, isNightTime = true)

        val caption = buildGalleryCaption(movement, utc)

        assertEquals("Night", caption.locationAndNight)
    }

    @Test
    fun buildGalleryCaption_locationAndNight_joinsBothWithSeparator() {
        val movement = BowelMovement(
            timestamp = timestamp,
            consistency = 4,
            location = Location.WALK,
            isNightTime = true
        )

        val caption = buildGalleryCaption(movement, utc)

        assertEquals("Walk · Night", caption.locationAndNight)
    }
}
