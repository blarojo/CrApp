package com.crapp.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers [FoodRepository.getOrCreateFood], the logic backing the food-logging
 * screen's dropdown + "Add new" flow: a never-before-seen name should be inserted
 * into the catalog, while a repeated name should resolve to the existing row rather
 * than creating a duplicate.
 */
@RunWith(AndroidJUnit4::class)
class FoodRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: FoodRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FoodRepository(db.foodDao(), db.foodEntryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getOrCreateFood_newName_createsCatalogEntry() = runBlocking {
        val id = repository.getOrCreateFood("Boiled Chicken")

        val stored = db.foodDao().getById(id)
        assertEquals("Boiled Chicken", stored?.name)
    }

    @Test
    fun getOrCreateFood_existingName_returnsSameIdWithoutDuplicating() = runBlocking {
        val firstId = repository.getOrCreateFood("Hill's I/D", brand = "Hill's")
        val secondId = repository.getOrCreateFood("Hill's I/D")

        assertEquals(firstId, secondId)
    }
}
