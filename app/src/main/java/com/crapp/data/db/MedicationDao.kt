package com.crapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crapp.data.model.Medication
import kotlinx.coroutines.flow.Flow

/** Mirrors [FoodDao] -- see [Medication]'s KDoc. */
@Dao
interface MedicationDao {
    /** Returns the new row id, or -1 if a medication with this [Medication.name] already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(medication: Medication): Long

    /** `medicationId` is `SET_NULL` on delete (see [com.crapp.data.model.MedicationEntry]), so this never fails/blocks. */
    @Delete
    suspend fun delete(medication: Medication)

    @Query("DELETE FROM medication")
    suspend fun deleteAll()

    @Query("SELECT * FROM medication WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Medication?

    @Query("SELECT * FROM medication WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Query("SELECT * FROM medication ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Medication>>

    /** Most-recently-logged first -- backs the medication-picker dropdown, same as [FoodDao.observeAllSortedByRecentUse]. */
    @Query(
        """
        SELECT medication.* FROM medication
        LEFT JOIN (
            SELECT medicationId, MAX(timestamp) AS lastUsed FROM medication_entry
            WHERE medicationId IS NOT NULL GROUP BY medicationId
        ) recent ON recent.medicationId = medication.id
        ORDER BY recent.lastUsed DESC, medication.name COLLATE NOCASE ASC
        """
    )
    fun observeAllSortedByRecentUse(): Flow<List<Medication>>
}
