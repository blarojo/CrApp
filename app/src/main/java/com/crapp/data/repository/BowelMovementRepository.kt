package com.crapp.data.repository

import com.crapp.data.db.BowelMovementDao
import com.crapp.data.model.BowelMovement
import kotlinx.coroutines.flow.Flow

class BowelMovementRepository(private val dao: BowelMovementDao) {
    val allMovements: Flow<List<BowelMovement>> = dao.observeAll()

    suspend fun getById(id: Long): BowelMovement? = dao.getById(id)

    suspend fun add(movement: BowelMovement): Long = dao.insert(movement)

    suspend fun update(movement: BowelMovement) = dao.update(movement)

    suspend fun delete(movement: BowelMovement) = dao.delete(movement)
}
