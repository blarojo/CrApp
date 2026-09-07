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
     * Adds a new food to the catalog directly, without going through a logged food
     * entry -- backs the Food Catalog's own "Add new food" flow (docs/backlog.md
     * spec 12's manual-entry addition), as opposed to [getOrCreateFood] which backs
     * the food-logging screen's inline "Add new" and never sets ingredients.
     *
     * If [name] already exists (e.g. it was auto-created via [getOrCreateFood] from a
     * log entry and never given ingredients), reuses that row and fills in whichever
     * of [brand]/[ingredients] are non-null, rather than silently no-op'ing or
     * creating a confusing duplicate-name row.
     */
    suspend fun addOrUpdateFood(name: String, brand: String?, ingredients: String?): Long {
        val existing = foodDao.getByName(name)
        val food = Food(
            id = existing?.id ?: 0,
            name = name,
            brand = brand ?: existing?.brand,
            ingredients = ingredients ?: existing?.ingredients
        )
        if (existing != null) {
            foodDao.update(food)
            return existing.id
        }
        val insertedId = foodDao.insert(food)
        if (insertedId != -1L) return insertedId
        // Insert was ignored (unique constraint) due to a race with a concurrent
        // identical insert -- same recovery as getOrCreateFood.
        return foodDao.getByName(name)?.id ?: error("Failed to add food '$name'")
    }

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
