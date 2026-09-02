package com.crapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.crapp.data.model.BowelMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface BowelMovementDao {
    @Insert
    suspend fun insert(bowelMovement: BowelMovement): Long

    @Update
    suspend fun update(bowelMovement: BowelMovement)

    @Delete
    suspend fun delete(bowelMovement: BowelMovement)

    @Query("SELECT * FROM bowel_movement ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BowelMovement>>

    @Query("SELECT * FROM bowel_movement WHERE id = :id")
    suspend fun getById(id: Long): BowelMovement?
}
