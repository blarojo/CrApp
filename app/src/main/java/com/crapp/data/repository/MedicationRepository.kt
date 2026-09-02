package com.crapp.data.repository

import com.crapp.data.db.MedicationEntryDao
import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val dao: MedicationEntryDao) {
    val allEntries: Flow<List<MedicationEntry>> = dao.observeAll()

    suspend fun getById(id: Long): MedicationEntry? = dao.getById(id)

    suspend fun add(entry: MedicationEntry): Long = dao.insert(entry)

    suspend fun update(entry: MedicationEntry) = dao.update(entry)

    suspend fun delete(entry: MedicationEntry) = dao.delete(entry)
}
