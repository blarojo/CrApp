package com.crapp.data.repository

import com.crapp.data.db.FoodDao
import com.crapp.data.db.FoodEntryDao
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import kotlinx.coroutines.flow.Flow

/** Outcome of [FoodRepository.deleteFood]. */
sealed class DeleteFoodResult {
    data object Success : DeleteFoodResult()
    data class InUse(val entryCount: Int) : DeleteFoodResult()
}

class FoodRepository(
    private val foodDao: FoodDao,
    private val foodEntryDao: FoodEntryDao
) {
    /** Backs the food-picker dropdown: most-recently-used foods first. */
    val foodsByRecentUse: Flow<List<Food>> = foodDao.observeAllSortedByRecentUse()

    /** Backs the Food Catalog screen (docs/development-plan.md Phase 8): alphabetical. */
    val allFoods: Flow<List<Food>> = foodDao.observeAll()

    val allFoodEntries: Flow<List<FoodEntry>> = foodEntryDao.observeAll()

    /**
     * Returns the id of an existing food matching [name] (case-sensitive exact
     * match), or inserts a new catalog entry and returns its id. Backs the
     * food-logging screen's "Add new" flow so picking a never-before-seen food
     * doesn't interrupt logging.
     */
    suspend fun getOrCreateFood(name: String, brand: String? = null): Long {
        foodDao.getByName(name)?.let { return it.id }
        val insertedId = foodDao.insert(Food(name = name, brand = brand))
        if (insertedId != -1L) return insertedId
        // Insert was ignored (unique constraint) due to a race with a concurrent
        // identical insert -- look it up again rather than fail.
        return foodDao.getByName(name)?.id
            ?: error("Failed to get or create food '$name'")
    }

    suspend fun getFoodEntryById(id: Long): FoodEntry? = foodEntryDao.getById(id)

    suspend fun getFoodById(id: Long): Food? = foodDao.getById(id)

    /** Updates a food's ingredients (or name/brand), e.g. from the Food Catalog screen. */
    suspend fun updateFood(food: Food) = foodDao.update(food)

    /**
     * Deletes [food] from the catalog -- backs the Food Catalog's "delete old ones"
     * flow. Refuses (returning [DeleteFoodResult.InUse] instead of deleting) if any
     * `food_entry` still references it, rather than letting the FK `RESTRICT`
     * constraint throw; the caller can then tell the user why.
     */
    suspend fun deleteFood(food: Food): DeleteFoodResult {
        val referencingCount = foodDao.countFoodEntriesReferencing(food.id)
        if (referencingCount > 0) return DeleteFoodResult.InUse(referencingCount)
        foodDao.delete(food)
        return DeleteFoodResult.Success
    }

    suspend fun logFoodEntry(entry: FoodEntry): Long = foodEntryDao.insert(entry)

    suspend fun updateFoodEntry(entry: FoodEntry) = foodEntryDao.update(entry)

    suspend fun deleteFoodEntry(entry: FoodEntry) = foodEntryDao.delete(entry)
}
