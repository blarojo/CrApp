package com.crapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crapp.data.model.FoodIngredient
import com.crapp.data.model.Ingredient
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    /** Returns the new row id, or -1 if this [Ingredient.name] already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ingredient: Ingredient): Long

    @Query("DELETE FROM ingredient")
    suspend fun deleteAll()

    @Query("SELECT * FROM ingredient WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Ingredient?

    @Query("SELECT * FROM ingredient ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Ingredient>>

    @Insert
    suspend fun insertFoodIngredient(foodIngredient: FoodIngredient): Long

    @Query("DELETE FROM food_ingredient")
    suspend fun deleteAllFoodIngredients()

    @Query("DELETE FROM food_ingredient WHERE foodId = :foodId")
    suspend fun deleteFoodIngredientsForFood(foodId: Long)

    @Query(
        """
        SELECT ingredient.* FROM ingredient
        INNER JOIN food_ingredient ON food_ingredient.ingredientId = ingredient.id
        WHERE food_ingredient.foodId = :foodId
        ORDER BY food_ingredient.position ASC
        """
    )
    fun observeIngredientsForFood(foodId: Long): Flow<List<Ingredient>>

    @Query("SELECT * FROM food_ingredient")
    suspend fun getAllFoodIngredients(): List<FoodIngredient>
}
