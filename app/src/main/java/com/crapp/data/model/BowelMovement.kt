package com.crapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A single logged bowel movement.
 *
 * [consistency] uses the Purina Fecal Scoring Chart (1 = very hard/dry, 7 = liquid,
 * no texture) -- the standard veterinary 7-point scale for dogs.
 */
@Entity(tableName = "bowel_movement")
data class BowelMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val consistency: Int,
    val color: String? = null,
    val hasBlood: Boolean = false,
    val hasMucus: Boolean = false,
    val notes: String? = null
)
