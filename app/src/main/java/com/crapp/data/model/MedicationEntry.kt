package com.crapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "medication_entry")
data class MedicationEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val name: String,
    val dose: String? = null,
    val notes: String? = null
)
