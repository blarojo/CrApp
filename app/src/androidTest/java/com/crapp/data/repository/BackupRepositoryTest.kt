package com.crapp.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.data.db.AppDatabase
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MealType
import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Exercises [BackupRepository] against a real (in-memory) Room database -- the
 * delete-then-insert transaction can't be verified with fakes, since it depends on
 * Room's actual foreign-key/transaction behavior.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var backupRepository: BackupRepository
    private lateinit var bowelMovementRepository: BowelMovementRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var medicationRepository: MedicationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bowelMovementRepository = BowelMovementRepository(db.bowelMovementDao())
        foodRepository = FoodRepository(db.foodDao(), db.foodEntryDao())
        medicationRepository = MedicationRepository(db.medicationEntryDao())
        backupRepository = BackupRepository(db, bowelMovementRepository, foodRepository, medicationRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedSampleData() {
        bowelMovementRepository.add(
            BowelMovement(timestamp = Instant.parse("2026-09-01T08:00:00Z"), consistency = 6, hasBlood = true)
        )
        val foodId = foodRepository.getOrCreateFood("Z/D", brand = "Hill's")
        foodRepository.updateFood(foodRepository.getFoodById(foodId)!!.copy(ingredients = "Chicken, water"))
        foodRepository.logFoodEntry(
            FoodEntry(timestamp = Instant.parse("2026-09-01T12:00:00Z"), foodId = foodId, mealType = MealType.MEAL)
        )
        medicationRepository.add(MedicationEntry(timestamp = Instant.parse("2026-09-01T09:00:00Z"), name = "Metronidazole"))
    }

    @Test
    fun exportThenRestore_roundTripsEverythingIncludingIngredientsAndForeignKeys() = runBlocking {
        seedSampleData()

        val json = backupRepository.exportToJson()
        backupRepository.clearAllData()
        backupRepository.restoreFromJson(json)

        assertEquals(1, bowelMovementRepository.allMovements.first().size)
        assertEquals(6, bowelMovementRepository.allMovements.first().first().consistency)

        val foods = foodRepository.allFoods.first()
        assertEquals(1, foods.size)
        assertEquals("Chicken, water", foods.first().ingredients)

        val foodEntries = foodRepository.allFoodEntries.first()
        assertEquals(1, foodEntries.size)
        // The restored food_entry's foodId must point at the restored food's id --
        // this is the part a naive delete-all/insert-all without matching ids would break.
        assertEquals(foods.first().id, foodEntries.first().foodId)

        assertEquals(1, medicationRepository.allEntries.first().size)
    }

    @Test
    fun clearAllData_wipesEveryTableIncludingFoodCatalog() = runBlocking {
        seedSampleData()

        backupRepository.clearAllData()

        assertTrue(bowelMovementRepository.allMovements.first().isEmpty())
        assertTrue(foodRepository.allFoods.first().isEmpty())
        assertTrue(foodRepository.allFoodEntries.first().isEmpty())
        assertTrue(medicationRepository.allEntries.first().isEmpty())
    }

    @Test
    fun restoreFromJson_invalidJson_throwsAndLeavesExistingDataUntouched() = runBlocking {
        seedSampleData()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { backupRepository.restoreFromJson("not a backup file") }
        }

        // The failed restore must not have touched what was already there.
        assertEquals(1, bowelMovementRepository.allMovements.first().size)
    }

    @Test
    fun restore_replacesRatherThanMerges() = runBlocking {
        seedSampleData()
        val backupWithOneMovement = backupRepository.exportToJson()

        // Add a second movement after the backup was taken.
        bowelMovementRepository.add(BowelMovement(timestamp = Instant.now(), consistency = 3))
        assertEquals(2, bowelMovementRepository.allMovements.first().size)

        backupRepository.restoreFromJson(backupWithOneMovement)

        // Restoring the earlier backup should bring the count back down to 1, not merge.
        assertEquals(1, bowelMovementRepository.allMovements.first().size)
    }
}
