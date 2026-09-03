package com.crapp.data.repository

import com.crapp.data.db.MedicationDao
import com.crapp.data.db.MedicationEntryDao
import com.crapp.data.model.Medication
import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.Flow

class MedicationRepository(
    private val dao: MedicationEntryDao,
    private val medicationDao: MedicationDao
) {
    val allEntries: Flow<List<MedicationEntry>> = dao.observeAll()

    /** Backs the medication-picker dropdown: most-recently-used medications first, mirroring [FoodRepository.foodsByRecentUse]. */
    val medicationsByRecentUse: Flow<List<Medication>> = medicationDao.observeAllSortedByRecentUse()

    /** Backs the Medication Catalog admin screen: alphabetical, mirroring [FoodRepository.allFoods]. */
    val allMedications: Flow<List<Medication>> = medicationDao.observeAll()

    suspend fun getById(id: Long): MedicationEntry? = dao.getById(id)

    suspend fun getMedicationById(id: Long): Medication? = medicationDao.getById(id)

    /**
     * Returns the id of an existing catalog medication matching [name] (case-sensitive
     * exact match), or inserts a new one and returns its id -- mirrors
     * [FoodRepository.getOrCreateFood], backing both the log screen's "Add new
     * medication" flow and lazily linking an older entry (logged before this catalog
     * existed, or restored from a backup) to a catalog row the first time it's edited.
     */
    suspend fun getOrCreateMedication(name: String): Long {
        medicationDao.getByName(name)?.let { return it.id }
        val insertedId = medicationDao.insert(Medication(name = name))
        if (insertedId != -1L) return insertedId
        return medicationDao.getByName(name)?.id
            ?: error("Failed to get or create medication '$name'")
    }

    /** Always succeeds -- `medicationId` is `SET_NULL` on delete, so no entries are ever blocked/orphaned. */
    suspend fun deleteMedication(medication: Medication) = medicationDao.delete(medication)

    suspend fun add(entry: MedicationEntry): Long = dao.insert(entry)

    suspend fun update(entry: MedicationEntry) = dao.update(entry)

    suspend fun delete(entry: MedicationEntry) = dao.delete(entry)
}
