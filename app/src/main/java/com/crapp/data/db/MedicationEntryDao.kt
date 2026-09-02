package com.crapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationEntryDao {
    @Insert
    suspend fun insert(medicationEntry: MedicationEntry): Long

    @Update
    suspend fun update(medicationEntry: MedicationEntry)

    @Delete
    suspend fun delete(medicationEntry: MedicationEntry)

    @Query("SELECT * FROM medication_entry ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MedicationEntry>>

    @Query("SELECT * FROM medication_entry WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntry?
}
