package com.crapp.ui.history

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.EnergyEntry
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MedicationEntry
import com.crapp.data.model.WalkEntry
import java.time.Instant

enum class HistoryEntryType {
    BOWEL_MOVEMENT,
    FOOD,
    MEDICATION,
    ENERGY,
    WALK
}

/**
 * Uniform wrapper over the loggable entity types so the History screen can render,
 * sort, and filter them as a single reverse-chronological feed.
 */
sealed class HistoryEntry {
    abstract val id: Long
    abstract val timestamp: Instant
    abstract val type: HistoryEntryType

    data class BowelMovementEntry(val movement: BowelMovement) : HistoryEntry() {
        override val id: Long get() = movement.id
        override val timestamp: Instant get() = movement.timestamp
        override val type: HistoryEntryType get() = HistoryEntryType.BOWEL_MOVEMENT
    }

    data class FoodLogEntry(val entry: FoodEntry, val food: Food?) : HistoryEntry() {
        override val id: Long get() = entry.id
        override val timestamp: Instant get() = entry.timestamp
        override val type: HistoryEntryType get() = HistoryEntryType.FOOD
    }

    data class MedicationLogEntry(val entry: MedicationEntry) : HistoryEntry() {
        override val id: Long get() = entry.id
        override val timestamp: Instant get() = entry.timestamp
        override val type: HistoryEntryType get() = HistoryEntryType.MEDICATION
    }

    data class EnergyLogEntry(val entry: EnergyEntry) : HistoryEntry() {
        override val id: Long get() = entry.id
        override val timestamp: Instant get() = entry.timestamp
        override val type: HistoryEntryType get() = HistoryEntryType.ENERGY
    }

    data class WalkLogEntry(val entry: WalkEntry) : HistoryEntry() {
        override val id: Long get() = entry.id
        override val timestamp: Instant get() = entry.timestamp
        override val type: HistoryEntryType get() = HistoryEntryType.WALK
    }
}
