package com.crapp.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [SeedDataCallback] directly against its own disposable in-memory
 * database -- deliberately not via [AppDatabase.getInstance] at all, so this can
 * never touch a real on-device `crapp.db` file.
 */
@RunWith(AndroidJUnit4::class)
class SeedDataCallbackTest {

    @Test
    fun onCreate_insertsAllFourSeedFoodsWithTheirIngredients() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(SeedDataCallback)
            .build()

        // Triggers Room to actually open/create the database, which is what fires
        // Callback.onCreate() -- a freshly-built Room instance doesn't create the
        // underlying SQLite file until first touched.
        val foods = db.foodDao().observeAll().first()

        assertEquals(SEED_FOODS.size, foods.size)
        assertEquals(SEED_FOODS.map { it.name }.toSet(), foods.map { it.name }.toSet())
        assertTrue(foods.all { !it.ingredients.isNullOrBlank() })

        db.close()
    }
}
