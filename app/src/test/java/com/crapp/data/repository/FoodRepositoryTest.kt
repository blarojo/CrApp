package com.crapp.data.repository

import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MealType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * Pure-JVM test of [FoodRepository]'s own orchestration logic (fake DAOs, no Room) --
 * complements the Room-backed instrumented FoodRepositoryTest in app/src/androidTest,
 * which exercises the real SQL (the recent-use join, unique-name constraint).
 */
class FoodRepositoryTest {

    private fun newRepo(): FoodRepository {
        val foodDao = FakeFoodDao()
        val foodEntryDao = FakeFoodEntryDao()
        foodDao.foodEntryDao = foodEntryDao // lets countFoodEntriesReferencing see real entry counts
        return FoodRepository(foodDao, foodEntryDao)
    }

    @Test
    fun getOrCreateFood_firstCall_insertsAndReturnsNewId() = runBlocking {
        val repo = newRepo()

        val id = repo.getOrCreateFood("Boiled Chicken")

        assertEquals("Boiled Chicken", repo.getFoodById(id)?.name)
    }

    @Test
    fun getOrCreateFood_existingName_returnsSameIdWithoutDuplicating() = runBlocking {
        val repo = newRepo()
        val firstId = repo.getOrCreateFood("Z/D")

        val secondId = repo.getOrCreateFood("Z/D")

        assertEquals(firstId, secondId)
    }

    @Test
    fun getOrCreateFood_differentNames_getDifferentIds() = runBlocking {
        val repo = newRepo()

        val chickenId = repo.getOrCreateFood("Boiled Chicken")
        val zdId = repo.getOrCreateFood("Z/D")

        assertNotEquals(chickenId, zdId)
    }

    @Test
    fun updateFood_persistsIngredients() = runBlocking {
        val repo = newRepo()
        val id = repo.getOrCreateFood("Z/D", brand = "Hill's")

        repo.updateFood(repo.getFoodById(id)!!.copy(ingredients = "Chicken, water"))

        assertEquals("Chicken, water", repo.getFoodById(id)?.ingredients)
    }

    @Test
    fun logFoodEntry_thenGetById_returnsIt() = runBlocking {
        val repo = newRepo()
        val foodId = repo.getOrCreateFood("Boiled Chicken")

        val entryId = repo.logFoodEntry(
            FoodEntry(timestamp = Instant.parse("2026-09-01T12:00:00Z"), foodId = foodId, mealType = MealType.MEAL)
        )

        assertEquals(foodId, repo.getFoodEntryById(entryId)?.foodId)
    }

    @Test
    fun deleteFoodEntry_removesIt() = runBlocking {
        val repo = newRepo()
        val foodId = repo.getOrCreateFood("Boiled Chicken")
        val entryId = repo.logFoodEntry(
            FoodEntry(timestamp = Instant.now(), foodId = foodId, mealType = MealType.TREAT)
        )

        repo.deleteFoodEntry(repo.getFoodEntryById(entryId)!!)

        assertNull(repo.getFoodEntryById(entryId))
    }

    @Test
    fun deleteFood_notReferencedByAnyEntry_deletesIt() = runBlocking {
        val repo = newRepo()
        val id = repo.getOrCreateFood("Boiled Chicken")

        val result = repo.deleteFood(repo.getFoodById(id)!!)

        assertEquals(DeleteFoodResult.Success, result)
        assertNull(repo.getFoodById(id))
    }

    @Test
    fun deleteFood_stillReferencedByAFoodEntry_refusesAndLeavesItIntact() = runBlocking {
        val repo = newRepo()
        val id = repo.getOrCreateFood("Boiled Chicken")
        repo.logFoodEntry(FoodEntry(timestamp = Instant.now(), foodId = id, mealType = MealType.MEAL))

        val result = repo.deleteFood(repo.getFoodById(id)!!)

        assertEquals(DeleteFoodResult.InUse(1), result)
        assertEquals("Boiled Chicken", repo.getFoodById(id)?.name)
    }

    @Test
    fun allFoods_reflectsCatalogInserts() = runBlocking {
        val repo = newRepo()
        repo.getOrCreateFood("Z/D")
        repo.getOrCreateFood("Boiled Chicken")

        val all = repo.allFoods.first()

        assertEquals(setOf("Z/D", "Boiled Chicken"), all.map { it.name }.toSet())
    }
}
