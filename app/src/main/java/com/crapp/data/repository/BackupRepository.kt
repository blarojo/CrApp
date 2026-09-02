package com.crapp.data.repository

import androidx.room.withTransaction
import com.crapp.data.backup.BackupData
import com.crapp.data.backup.BackupSerializer
import com.crapp.data.db.AppDatabase
import kotlinx.coroutines.flow.first

/**
 * Full-database backup/restore (docs/development-plan.md Phase 8), distinct from the
 * vet-facing CSV export (§8): this preserves every row's id and foreign keys
 * exactly, so a backup restores byte-for-byte. An app *update* alone (installing a
 * new version over an existing install) already preserves the on-device database
 * without needing this -- this is the safety net for the cases that don't: an
 * uninstall, a device change, or a phone reset.
 */
class BackupRepository(
    private val database: AppDatabase,
    private val bowelMovementRepository: BowelMovementRepository,
    private val foodRepository: FoodRepository,
    private val medicationRepository: MedicationRepository
) {
    suspend fun exportToJson(): String {
        val data = BackupData(
            bowelMovements = bowelMovementRepository.allMovements.first(),
            foods = foodRepository.foodsByRecentUse.first(),
            foodEntries = foodRepository.allFoodEntries.first(),
            medicationEntries = medicationRepository.allEntries.first()
        )
        return BackupSerializer.serialize(data)
    }

    /**
     * Replaces everything currently in the database with [json]'s contents, atomically
     * -- an all-or-nothing restore, not a merge.
     * @throws IllegalArgumentException if [json] isn't a recognizable CrApp backup.
     */
    suspend fun restoreFromJson(json: String) {
        val data = BackupSerializer.deserialize(json)
        database.withTransaction {
            deleteAllTables()
            database.foodDao().insertAll(data.foods)
            database.bowelMovementDao().insertAll(data.bowelMovements)
            database.foodEntryDao().insertAll(data.foodEntries)
            database.medicationEntryDao().insertAll(data.medicationEntries)
        }
    }

    /** Wipes every table -- bowel movements, food entries, the food catalog, medications. */
    suspend fun clearAllData() {
        database.withTransaction { deleteAllTables() }
    }

    // Children before parents, so the food_entry -> food foreign key is never
    // briefly dangling; caller wraps this in a transaction.
    private suspend fun deleteAllTables() {
        database.foodEntryDao().deleteAll()
        database.bowelMovementDao().deleteAll()
        database.foodDao().deleteAll()
        database.medicationEntryDao().deleteAll()
    }
}
