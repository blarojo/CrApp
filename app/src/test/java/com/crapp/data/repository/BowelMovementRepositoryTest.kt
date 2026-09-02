package com.crapp.data.repository

import com.crapp.data.model.BowelMovement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class BowelMovementRepositoryTest {

    @Test
    fun add_thenGetById_returnsTheStoredMovement() = runBlocking {
        val repo = BowelMovementRepository(FakeBowelMovementDao())

        val id = repo.add(BowelMovement(timestamp = Instant.parse("2026-09-01T08:00:00Z"), consistency = 5))

        val stored = repo.getById(id)
        assertEquals(5, stored?.consistency)
    }

    @Test
    fun update_persistsChangedFields() = runBlocking {
        val repo = BowelMovementRepository(FakeBowelMovementDao())
        val id = repo.add(BowelMovement(timestamp = Instant.now(), consistency = 3))

        repo.update(repo.getById(id)!!.copy(consistency = 7, notes = "watery"))

        val updated = repo.getById(id)
        assertEquals(7, updated?.consistency)
        assertEquals("watery", updated?.notes)
    }

    @Test
    fun delete_removesTheMovement() = runBlocking {
        val repo = BowelMovementRepository(FakeBowelMovementDao())
        val id = repo.add(BowelMovement(timestamp = Instant.now(), consistency = 4))

        repo.delete(repo.getById(id)!!)

        assertNull(repo.getById(id))
    }

    @Test
    fun allMovements_reflectsInsertsNewestFirst() = runBlocking {
        val repo = BowelMovementRepository(FakeBowelMovementDao())
        repo.add(BowelMovement(timestamp = Instant.parse("2026-09-01T08:00:00Z"), consistency = 2))
        repo.add(BowelMovement(timestamp = Instant.parse("2026-09-02T08:00:00Z"), consistency = 6))

        val all = repo.allMovements.first()

        assertEquals(2, all.size)
        assertEquals(6, all[0].consistency)
    }
}
