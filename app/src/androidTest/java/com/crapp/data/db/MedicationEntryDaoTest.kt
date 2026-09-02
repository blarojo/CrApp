package com.crapp.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class MedicationEntryDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: MedicationEntryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.medicationEntryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertUpdateDelete_roundTripsCorrectly() = runBlocking {
        val id = dao.insert(
            MedicationEntry(timestamp = Instant.parse("2026-09-01T08:00:00Z"), name = "Metronidazole", dose = "250mg")
        )

        val saved = dao.getById(id)!!
        assertEquals("Metronidazole", saved.name)
        assertEquals("250mg", saved.dose)

        dao.update(saved.copy(dose = "500mg", notes = "increased per vet"))
        val updated = dao.getById(id)!!
        assertEquals("500mg", updated.dose)
        assertEquals("increased per vet", updated.notes)

        dao.delete(updated)
        assertEquals(0, dao.observeAll().first().size)
    }

    @Test
    fun observeAll_ordersNewestFirst() = runBlocking {
        dao.insert(MedicationEntry(timestamp = Instant.parse("2026-09-01T08:00:00Z"), name = "Metronidazole"))
        dao.insert(MedicationEntry(timestamp = Instant.parse("2026-09-02T08:00:00Z"), name = "Probiotic"))

        val all = dao.observeAll().first()
        assertEquals("Probiotic", all[0].name)
        assertEquals("Metronidazole", all[1].name)
    }
}
