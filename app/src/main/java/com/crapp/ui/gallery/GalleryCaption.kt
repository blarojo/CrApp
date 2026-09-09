package com.crapp.ui.gallery

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Location
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Same pattern as `HistoryScreen`'s own formatter -- one voice for dates across the app. */
val galleryDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

/**
 * The up-to-3-line caption shown under a Gallery photo card, and repeated below the
 * full-size viewer (docs/backlog.md spec 14). [locationAndNight] is null when there's
 * nothing to show -- same "omit an empty line" rule used throughout this app's cards,
 * rather than rendering a blank third line.
 */
data class GalleryCaption(
    val dateTime: String,
    val consistencyAndAmount: String,
    val locationAndNight: String?
)

/**
 * Pulled out as pure logic (no Compose/Android dependency) specifically so the
 * "what shows, what's omitted" rules are unit-testable -- same reasoning as
 * [com.crapp.util.countBowelMovementsToday]. Deliberately mirrors `HistoryScreen`'s
 * bowel-movement subtitle wording (same `displayName`s, same "Consistency N" phrasing)
 * rather than inventing new copy, but leaves out notes/blood/mucus flags -- a gallery
 * card's job is "which day, roughly what happened", not a full record (History still
 * covers that).
 */
fun buildGalleryCaption(movement: BowelMovement, zone: ZoneId = ZoneId.systemDefault()): GalleryCaption {
    val consistencyAndAmount = "Consistency ${movement.consistency}" +
        (movement.amount?.let { " · ${it.displayName}" } ?: "")

    val locationAndNightParts = buildList {
        when (movement.location) {
            Location.OTHER -> add(movement.locationOther?.let { "Other: $it" } ?: "Other")
            null -> {}
            else -> add(movement.location.displayName)
        }
        if (movement.isNightTime) add("Night")
    }

    return GalleryCaption(
        dateTime = galleryDateTimeFormatter.format(movement.timestamp.atZone(zone)),
        consistencyAndAmount = consistencyAndAmount,
        locationAndNight = locationAndNightParts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    )
}
