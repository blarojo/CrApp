package com.crapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A single logged bowel movement.
 *
 * [consistency] uses the Purina Fecal Scoring Chart (1 = very hard/dry, 7 = liquid,
 * no texture) -- the standard veterinary 7-point scale for dogs.
 *
 * [amount], [location]/[locationOther], [isNightTime], and [photoUri] are all from
 * docs/future-features.md, added together in schema version 3
 * ([MIGRATION_2_3][com.crapp.data.db.MIGRATION_2_3]) -- all nullable/defaulted so
 * existing rows stay valid without a fabricated value.
 */
@Entity(tableName = "bowel_movement")
data class BowelMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val consistency: Int,
    val color: String? = null,
    val hasBlood: Boolean = false,
    val hasMucus: Boolean = false,
    val notes: String? = null,
    /** How much was passed. Independent of [consistency], not derived from it. */
    val amount: Amount? = null,
    /** Where it happened. */
    val location: Location? = null,
    /** Free text, only meaningful when [location] == [Location.OTHER]. */
    val locationOther: String? = null,
    /**
     * Derived from [timestamp] against the configurable night-window setting at save
     * time, not hand-entered. Non-nullable with a `false` default so a migrated
     * historical row (no way to know retroactively) reads as "not night" rather than
     * an unknown tri-state.
     */
    val isNightTime: Boolean = false,
    /**
     * A `content://` `MediaStore` URI into the shared `Pictures/CrApp` album.
     * Deliberately not app-private storage; see
     * [com.crapp.export.BowelMovementPhotoStore].
     */
    val photoUri: String? = null
)
