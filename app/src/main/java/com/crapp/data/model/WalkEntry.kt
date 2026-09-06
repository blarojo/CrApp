package com.crapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A dog walker's report of a walk: only a count, no per-movement detail
 * (docs/future-features.md spec 5). Deliberately separate from [BowelMovement] rows
 * -- kept alongside (not merged with) [Location.WALK]-tagged individual movements,
 * which cover the case where the user herself walks the dog and logs each movement.
 * See the in-app warning on the logging screen for avoiding double-counting.
 */
@Entity(tableName = "walk_entry")
data class WalkEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val bowelMovementCount: Int,
    val notes: String? = null
)
