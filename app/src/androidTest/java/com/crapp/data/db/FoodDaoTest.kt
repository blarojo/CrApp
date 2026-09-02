package com.crapp.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MealType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class FoodDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var foodDao: FoodDao
    private lateinit var foodEntryDao: FoodEntryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        foodDao = db.foodDao()
        foodEntryDao = db.foodEntryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_duplicateName_isIgnoredAndOriginalRemainsFindable() = runBlocking {
        val firstId = foodDao.insert(Food(name = "Boiled Chicken"))
        val secondAttemptId = foodDao.insert(Food(name = "Boiled Chicken"))

        assertEquals(-1L, secondAttemptId)
        assertEquals(firstId, foodDao.getByName("Boiled Chicken")?.id)
    }

    @Test
    fun observeAllSortedByRecentUse_mostRecentlyLoggedFoodFirst() = runBlocking {
        val chickenId = foodDao.insert(Food(name = "Boiled Chicken"))
        val kibbleId = foodDao.insert(Food(name = "Hill's I/D"))

        foodEntryDao.insert(
            FoodEntry(timestamp = Instant.parse("2026-09-01T08:00:00Z"), foodId = chickenId, mealType = MealType.MEAL)
        )
        foodEntryDao.insert(
            FoodEntry(timestamp = Instant.parse("2026-09-02T08:00:00Z"), foodId = kibbleId, mealType = MealType.MEAL)
        )

        val ordered = foodDao.observeAllSortedByRecentUse().first()
        assertEquals(kibbleId, ordered[0].id)
        assertEquals(chickenId, ordered[1].id)
    }

    @Test
    fun observeAllSortedByRecentUse_neverLoggedFoodsSortAlphabeticallyAfterUsedOnes() = runBlocking {
        val kibbleId = foodDao.insert(Food(name = "Hill's I/D"))
        foodEntryDao.insert(
            FoodEntry(timestamp = Instant.now(), foodId = kibbleId, mealType = MealType.MEAL)
        )
        foodDao.insert(Food(name = "Zucchini"))
        foodDao.insert(Food(name = "Apple"))

        val ordered = foodDao.observeAllSortedByRecentUse().first()
        assertEquals("Hill's I/D", ordered[0].name)
        assertEquals("Apple", ordered[1].name)
        assertEquals("Zucchini", ordered[2].name)
    }
}
