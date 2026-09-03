package com.crapp.data.repository

import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MedicationRepositoryTest {

    @Test
    fun add_thenGetById_returnsIt() = runBlocking {
        val repo = MedicationRepository(FakeMedicationEntryDao(), FakeMedicationDao())

        val id = repo.add(MedicationEntry(timestamp = Instant.now(), name = "Metronidazole", dose = "250mg"))

        assertEquals("Metronidazole", repo.getById(id)?.name)
    }

    @Test
    fun update_persistsChangedDose() = runBlocking {
        val repo = MedicationRepository(FakeMedicationEntryDao(), FakeMedicationDao())
        val id = repo.add(MedicationEntry(timestamp = Instant.now(), name = "Metronidazole", dose = "250mg"))

        repo.update(repo.getById(id)!!.copy(dose = "500mg"))

        assertEquals("500mg", repo.getById(id)?.dose)
    }

    @Test
    fun delete_removesIt() = runBlocking {
        val repo = MedicationRepository(FakeMedicationEntryDao(), FakeMedicationDao())
        val id = repo.add(MedicationEntry(timestamp = Instant.now(), name = "Metronidazole"))

        repo.delete(repo.getById(id)!!)

        assertNull(repo.getById(id))
    }

    @Test
    fun allEntries_reflectsInserts() = runBlocking {
        val repo = MedicationRepository(FakeMedicationEntryDao(), FakeMedicationDao())
        repo.add(MedicationEntry(timestamp = Instant.parse("2026-09-01T09:00:00Z"), name = "Metronidazole"))
        repo.add(MedicationEntry(timestamp = Instant.parse("2026-09-02T09:00:00Z"), name = "Probiotic"))

        val all = repo.allEntries.first()

        assertEquals(2, all.size)
        assertEquals("Probiotic", all[0].name)
    }
}
