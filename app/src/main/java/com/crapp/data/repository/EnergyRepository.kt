package com.crapp.data.repository

import com.crapp.data.db.EnergyEntryDao
import com.crapp.data.model.EnergyEntry
import kotlinx.coroutines.flow.Flow

class EnergyRepository(private val dao: EnergyEntryDao) {
    val allEntries: Flow<List<EnergyEntry>> = dao.observeAll()

    suspend fun getById(id: Long): EnergyEntry? = dao.getById(id)

    suspend fun add(entry: EnergyEntry): Long = dao.insert(entry)

    suspend fun update(entry: EnergyEntry) = dao.update(entry)

    suspend fun delete(entry: EnergyEntry) = dao.delete(entry)
}
