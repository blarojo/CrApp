package com.crapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crapp.data.model.Food
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    /** Returns the new row id, or -1 if a food with this [Food.name] already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(food: Food): Long

    @Update
    suspend fun update(food: Food)

    /**
     * Backs the Food Catalog's "delete old ones" flow. `food_entry.foodId` is a
     * `RESTRICT` foreign key (see [com.crapp.data.model.FoodEntry]'s KDoc), so this
     * throws `android.database.sqlite.SQLiteConstraintException` if any food_entry
     * still references [food] -- the caller (FoodRepository) surfaces that as a
     * friendly "still in use" message rather than silently orphaning history.
     */
    @Delete
    suspend fun delete(food: Food)

    @Query("SELECT COUNT(*) FROM food_entry WHERE foodId = :foodId")
    suspend fun countFoodEntriesReferencing(foodId: Long): Int

    /** Bulk insert for backup restore (docs/development-plan.md Phase 8); preserves ids. */
    @Insert
    suspend fun insertAll(foods: List<Food>)

    @Query("DELETE FROM food")
    suspend fun deleteAll()

    @Query("SELECT * FROM food WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Food?

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Long): Food?

    @Query("SELECT * FROM food ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Food>>

    /**
     * Foods ordered by most-recently-logged first (foods never logged sort
     * alphabetically after all used foods). Backs the food-picker dropdown so the
     * common cases are one tap.
     */
    @Query(
        """
        SELECT food.* FROM food
        LEFT JOIN (
            SELECT foodId, MAX(timestamp) AS lastUsed FROM food_entry GROUP BY foodId
        ) recent ON recent.foodId = food.id
        ORDER BY recent.lastUsed DESC, food.name COLLATE NOCASE ASC
        """
    )
    fun observeAllSortedByRecentUse(): Flow<List<Food>>
}
