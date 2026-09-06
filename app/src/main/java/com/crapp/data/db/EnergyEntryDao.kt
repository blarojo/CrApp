package com.crapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.crapp.data.model.EnergyEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyEntryDao {
    @Insert
    suspend fun insert(entry: EnergyEntry): Long

    /** Bulk insert for backup restore; preserves ids. */
    @Insert
    suspend fun insertAll(entries: List<EnergyEntry>)

    @Query("DELETE FROM energy_entry")
    suspend fun deleteAll()

    @Update
    suspend fun update(entry: EnergyEntry)

    @Delete
    suspend fun delete(entry: EnergyEntry)

    @Query("SELECT * FROM energy_entry ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EnergyEntry>>

    @Query("SELECT * FROM energy_entry WHERE id = :id")
    suspend fun getById(id: Long): EnergyEntry?
}
