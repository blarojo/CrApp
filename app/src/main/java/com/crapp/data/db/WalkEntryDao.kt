package com.crapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.crapp.data.model.WalkEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkEntryDao {
    @Insert
    suspend fun insert(entry: WalkEntry): Long

    /** Bulk insert for backup restore; preserves ids. */
    @Insert
    suspend fun insertAll(entries: List<WalkEntry>)

    @Query("DELETE FROM walk_entry")
    suspend fun deleteAll()

    @Update
    suspend fun update(entry: WalkEntry)

    @Delete
    suspend fun delete(entry: WalkEntry)

    @Query("SELECT * FROM walk_entry ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<WalkEntry>>

    @Query("SELECT * FROM walk_entry WHERE id = :id")
    suspend fun getById(id: Long): WalkEntry?
}
