package com.crapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * [medicationId] links to the [Medication] catalog picked via the log screen's
 * dropdown -- nullable and `SET_NULL` on delete (unlike [FoodEntry.foodId], which
 * `RESTRICT`s): [name] is a denormalized copy of the medication's name at the time
 * it was logged, so a row stays fully readable/exportable even if its catalog
 * entry is later deleted, or (for entries logged before this field existed, or
 * restored from an older backup) was never linked to one at all.
 */
@Entity(
    tableName = "medication_entry",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("medicationId")]
)
data class MedicationEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val name: String,
    val dose: String? = null,
    val notes: String? = null,
    /**
     * Structured dose, additive to (not replacing) the free-text [dose] --
     * docs/future-features.md's dose/amount spec. Only meaningful together with
     * [doseUnit]; both null means "not recorded structurally," not zero.
     */
    val doseValue: Double? = null,
    /** Unit for [doseValue], e.g. "mg", "ml". Free text, not an enum -- units vary too much to enumerate. */
    val doseUnit: String? = null,
    val medicationId: Long? = null
)
