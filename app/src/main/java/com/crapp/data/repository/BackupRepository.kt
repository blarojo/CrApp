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
    private val medicationRepository: MedicationRepository,
    private val energyRepository: EnergyRepository,
    private val walkRepository: WalkRepository
) {
    suspend fun exportToJson(): String {
        val data = BackupData(
            bowelMovements = bowelMovementRepository.allMovements.first(),
            foods = foodRepository.foodsByRecentUse.first(),
            foodEntries = foodRepository.allFoodEntries.first(),
            medicationEntries = medicationRepository.allEntries.first(),
            energyEntries = energyRepository.allEntries.first(),
            walkEntries = walkRepository.allEntries.first(),
            medications = medicationRepository.allMedications.first()
        )
        return BackupSerializer.serialize(data)
    }

    /**
     * Replaces bowel movements, medication entries, energy entries, and walk entries
     * with [json]'s contents, atomically -- an all-or-nothing restore of those
     * tables, not a merge.
     *
     * **Deliberately leaves the Food Catalog and Medication Catalog (and food log
     * entries) untouched.** Both catalogs are curated, admin-managed reference data
     * (see `ui/foodcatalog`/`ui/medicationcatalog`) rather than something you'd want
     * silently replaced by whatever was in an old backup -- restoring a backup's
     * stale catalog over a freshly-cleaned-up one was exactly the problem this was
     * built to avoid.
     *
     * The tradeoffs this creates, and how each is handled:
     *  - A backup's `food_entry` rows reference the backup's *own* food ids, which
     *    won't line up with today's (kept) catalog ids -- restoring them would point
     *    at the wrong food, or fail the foreign key outright. So food log history
     *    isn't part of this restore either; re-add it manually if you need it back.
     *  - `medication_entry.medicationId` has the same id-mismatch problem, but
     *    that column is nullable and `SET_NULL` (see [com.crapp.data.model.MedicationEntry]),
     *    so restored medication entries are simply inserted **unlinked** from any
     *    catalog row (`medicationId = null`) rather than skipped -- `name`/`dose`
     *    are denormalized onto the row already, so nothing is lost, and editing a
     *    restored entry lazily re-links it into the (kept) catalog by name.
     *
     * The structured-ingredient tables
     * ([com.crapp.data.model.Ingredient]/[com.crapp.data.model.FoodIngredient]) are
     * derived from the (untouched) Food Catalog's `ingredients` text, so they don't
     * need any special handling here either.
     * @throws IllegalArgumentException if [json] isn't a recognizable CrApp backup.
     */
    suspend fun restoreFromJson(json: String) {
        val data = BackupSerializer.deserialize(json)
        database.withTransaction {
            database.bowelMovementDao().deleteAll()
            database.medicationEntryDao().deleteAll()
            database.energyEntryDao().deleteAll()
            database.walkEntryDao().deleteAll()

            database.bowelMovementDao().insertAll(data.bowelMovements)
            database.medicationEntryDao().insertAll(data.medicationEntries.map { it.copy(medicationId = null) })
            database.energyEntryDao().insertAll(data.energyEntries)
            database.walkEntryDao().insertAll(data.walkEntries)
        }
    }

    /** Wipes every table -- bowel movements, food/medication catalogs and entries, and the rest. */
    suspend fun clearAllData() {
        database.withTransaction { deleteAllTables() }
    }

    // Children before parents, so a foreign key is never briefly dangling; caller
    // wraps this in a transaction.
    private suspend fun deleteAllTables() {
        database.ingredientDao().deleteAllFoodIngredients()
        database.ingredientDao().deleteAll()
        database.foodEntryDao().deleteAll()
        database.bowelMovementDao().deleteAll()
        database.foodDao().deleteAll()
        database.medicationEntryDao().deleteAll()
        database.medicationDao().deleteAll()
        database.energyEntryDao().deleteAll()
        database.walkEntryDao().deleteAll()
    }
}
