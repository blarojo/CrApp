package com.crapp.data.repository

import com.crapp.data.db.WalkEntryDao
import com.crapp.data.model.WalkEntry
import kotlinx.coroutines.flow.Flow

class WalkRepository(private val dao: WalkEntryDao) {
    val allEntries: Flow<List<WalkEntry>> = dao.observeAll()

    suspend fun getById(id: Long): WalkEntry? = dao.getById(id)

    suspend fun add(entry: WalkEntry): Long = dao.insert(entry)

    suspend fun update(entry: WalkEntry) = dao.update(entry)

    suspend fun delete(entry: WalkEntry) = dao.delete(entry)
}
