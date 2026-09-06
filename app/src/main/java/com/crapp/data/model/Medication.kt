package com.crapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Catalog of known medications, mirroring [Food]'s catalog -- lets the
 * medication-logging screen offer a dropdown of previously-used medications
 * instead of retyping the name every time (per the user's request to bring
 * medication logging in line with how food logging already works), and gives a
 * dedicated admin screen (`ui/medicationcatalog`) a place to delete stale entries.
 */
@Entity(
    tableName = "medication",
    indices = [Index(value = ["name"], unique = true)]
)
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String? = null
)
