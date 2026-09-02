package com.crapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.crapp.data.model.FoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Insert
    suspend fun insert(foodEntry: FoodEntry): Long

    @Update
    suspend fun update(foodEntry: FoodEntry)

    @Delete
    suspend fun delete(foodEntry: FoodEntry)

    @Query("SELECT * FROM food_entry ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entry WHERE id = :id")
    suspend fun getById(id: Long): FoodEntry?
}
