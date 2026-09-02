package com.crapp.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.data.model.BowelMovement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class BowelMovementDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: BowelMovementDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bowelMovementDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserveAll_ordersNewestFirst() = runBlocking {
        val older = BowelMovement(timestamp = Instant.parse("2026-09-01T08:00:00Z"), consistency = 2)
        val newer = BowelMovement(
            timestamp = Instant.parse("2026-09-02T08:00:00Z"),
            consistency = 5,
            hasBlood = true,
            notes = "watery"
        )

        dao.insert(older)
        dao.insert(newer)

        val all = dao.observeAll().first()
        assertEquals(2, all.size)
        assertEquals(5, all[0].consistency)
        assertTrue(all[0].hasBlood)
        assertEquals(2, all[1].consistency)
    }

    @Test
    fun update_persistsChangedFields() = runBlocking {
        val id = dao.insert(BowelMovement(timestamp = Instant.now(), consistency = 3))
        val saved = dao.getById(id)!!

        dao.update(saved.copy(consistency = 6, notes = "updated"))

        val updated = dao.getById(id)!!
        assertEquals(6, updated.consistency)
        assertEquals("updated", updated.notes)
    }

    @Test
    fun delete_removesMovement() = runBlocking {
        val id = dao.insert(BowelMovement(timestamp = Instant.now(), consistency = 4))
        val saved = dao.getById(id)!!

        dao.delete(saved)

        assertEquals(0, dao.observeAll().first().size)
    }
}
