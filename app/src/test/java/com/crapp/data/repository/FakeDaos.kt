package com.crapp.data.repository

import com.crapp.data.db.BowelMovementDao
import com.crapp.data.db.FoodDao
import com.crapp.data.db.FoodEntryDao
import com.crapp.data.db.MedicationDao
import com.crapp.data.db.MedicationEntryDao
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.Medication
import com.crapp.data.model.MedicationEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Hand-rolled in-memory fakes of the DAOs, so repository *orchestration* logic
 * (dedupe/race handling, forwarding, etc.) can be unit tested on the plain JVM
 * without Room/Android -- no mocking library needed for interfaces this small.
 * SQL correctness itself (joins, ordering) stays covered by the Room-backed
 * instrumented DAO tests in app/src/androidTest.
 */
class FakeBowelMovementDao : BowelMovementDao {
    private val rows = linkedMapOf<Long, BowelMovement>()
    private var nextId = 1L
    private val _all = MutableStateFlow<List<BowelMovement>>(emptyList())

    override suspend fun insert(bowelMovement: BowelMovement): Long {
        val id = if (bowelMovement.id != 0L) bowelMovement.id else nextId++
        rows[id] = bowelMovement.copy(id = id)
        publish()
        return id
    }

    override suspend fun insertAll(bowelMovements: List<BowelMovement>) {
        bowelMovements.forEach { insert(it) }
    }

    override suspend fun deleteAll() {
        rows.clear()
        publish()
    }

    override suspend fun update(bowelMovement: BowelMovement) {
        rows[bowelMovement.id] = bowelMovement
        publish()
    }

    override suspend fun delete(bowelMovement: BowelMovement) {
        rows.remove(bowelMovement.id)
        publish()
    }

    override fun observeAll(): StateFlow<List<BowelMovement>> = _all

    override suspend fun getById(id: Long): BowelMovement? = rows[id]

    private fun publish() {
        _all.value = rows.values.sortedByDescending { it.timestamp }
    }
}

class FakeFoodDao : FoodDao {
    private val rows = linkedMapOf<Long, Food>()
    private var nextId = 1L
    // Shared, continuously-updated flows -- FoodRepository reads observeAll()/
    // observeAllSortedByRecentUse() exactly once (into a `val`) at construction time,
    // same as a real Room-backed Flow would, so a fresh snapshot per call (as opposed
    // to a shared mutable one updated on every write) would never reflect later inserts.
    private val _all = MutableStateFlow<List<Food>>(emptyList())
    private val _allByRecentUse = MutableStateFlow<List<Food>>(emptyList())

    var foodEntryDao: FakeFoodEntryDao? = null

    override suspend fun insert(food: Food): Long {
        if (rows.values.any { it.name == food.name }) return -1L
        val id = if (food.id != 0L) food.id else nextId++
        rows[id] = food.copy(id = id)
        publish()
        return id
    }

    override suspend fun update(food: Food) {
        rows[food.id] = food
        publish()
    }

    override suspend fun delete(food: Food) {
        rows.remove(food.id)
        publish()
    }

    override suspend fun countFoodEntriesReferencing(foodId: Long): Int =
        foodEntryDao?.countByFoodId(foodId) ?: 0

    override suspend fun insertAll(foods: List<Food>) {
        foods.forEach { f -> rows[if (f.id != 0L) f.id else nextId++] = f }
        publish()
    }

    override suspend fun deleteAll() {
        rows.clear()
        publish()
    }

    override suspend fun getByName(name: String): Food? = rows.values.firstOrNull { it.name == name }

    override suspend fun getById(id: Long): Food? = rows[id]

    override fun observeAll(): StateFlow<List<Food>> = _all

    override fun observeAllSortedByRecentUse(): StateFlow<List<Food>> = _allByRecentUse

    private fun publish() {
        val sorted = rows.values.sortedBy { it.name.lowercase() }
        _all.value = sorted
        _allByRecentUse.value = sorted
    }
}

class FakeFoodEntryDao : FoodEntryDao {
    private val rows = linkedMapOf<Long, FoodEntry>()
    private var nextId = 1L
    private val _all = MutableStateFlow<List<FoodEntry>>(emptyList())

    override suspend fun insert(foodEntry: FoodEntry): Long {
        val id = if (foodEntry.id != 0L) foodEntry.id else nextId++
        rows[id] = foodEntry.copy(id = id)
        publish()
        return id
    }

    override suspend fun insertAll(foodEntries: List<FoodEntry>) {
        foodEntries.forEach { insert(it) }
    }

    override suspend fun deleteAll() {
        rows.clear()
        publish()
    }

    override suspend fun update(foodEntry: FoodEntry) {
        rows[foodEntry.id] = foodEntry
        publish()
    }

    override suspend fun delete(foodEntry: FoodEntry) {
        rows.remove(foodEntry.id)
        publish()
    }

    override fun observeAll(): StateFlow<List<FoodEntry>> = _all

    /** Test-only helper backing [FakeFoodDao.countFoodEntriesReferencing] -- not part of the real [FoodEntryDao] interface. */
    fun countByFoodId(foodId: Long): Int = rows.values.count { it.foodId == foodId }

    override suspend fun getById(id: Long): FoodEntry? = rows[id]

    private fun publish() {
        _all.value = rows.values.sortedByDescending { it.timestamp }
    }
}

class FakeMedicationEntryDao : MedicationEntryDao {
    private val rows = linkedMapOf<Long, MedicationEntry>()
    private var nextId = 1L
    private val _all = MutableStateFlow<List<MedicationEntry>>(emptyList())

    override suspend fun insert(medicationEntry: MedicationEntry): Long {
        val id = if (medicationEntry.id != 0L) medicationEntry.id else nextId++
        rows[id] = medicationEntry.copy(id = id)
        publish()
        return id
    }

    override suspend fun insertAll(medicationEntries: List<MedicationEntry>) {
        medicationEntries.forEach { insert(it) }
    }

    override suspend fun deleteAll() {
        rows.clear()
        publish()
    }

    override suspend fun update(medicationEntry: MedicationEntry) {
        rows[medicationEntry.id] = medicationEntry
        publish()
    }

    override suspend fun delete(medicationEntry: MedicationEntry) {
        rows.remove(medicationEntry.id)
        publish()
    }

    override fun observeAll(): StateFlow<List<MedicationEntry>> = _all

    override suspend fun getById(id: Long): MedicationEntry? = rows[id]

    private fun publish() {
        _all.value = rows.values.sortedByDescending { it.timestamp }
    }
}

class FakeMedicationDao : MedicationDao {
    private val rows = linkedMapOf<Long, Medication>()
    private var nextId = 1L
    private val _all = MutableStateFlow<List<Medication>>(emptyList())
    private val _allByRecentUse = MutableStateFlow<List<Medication>>(emptyList())

    override suspend fun insert(medication: Medication): Long {
        if (rows.values.any { it.name == medication.name }) return -1L
        val id = if (medication.id != 0L) medication.id else nextId++
        rows[id] = medication.copy(id = id)
        publish()
        return id
    }

    override suspend fun delete(medication: Medication) {
        rows.remove(medication.id)
        publish()
    }

    override suspend fun deleteAll() {
        rows.clear()
        publish()
    }

    override suspend fun getByName(name: String): Medication? = rows.values.firstOrNull { it.name == name }

    override suspend fun getById(id: Long): Medication? = rows[id]

    override fun observeAll(): StateFlow<List<Medication>> = _all

    override fun observeAllSortedByRecentUse(): StateFlow<List<Medication>> = _allByRecentUse

    private fun publish() {
        val sorted = rows.values.sortedBy { it.name.lowercase() }
        _all.value = sorted
        _allByRecentUse.value = sorted
    }
}
