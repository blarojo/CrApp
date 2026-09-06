package com.crapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** A single day's/moment's energy-level read (docs/future-features.md spec 4). */
@Entity(tableName = "energy_entry")
data class EnergyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val level: EnergyLevel,
    val notes: String? = null
)
