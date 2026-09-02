package com.crapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Catalog of known foods, built up as they're logged. [FoodEntry] rows reference a
 * [Food] by id so the food-logging screen can offer a dropdown of previously-used
 * foods instead of requiring free text every time.
 */
@Entity(
    tableName = "food",
    indices = [Index(value = ["name"], unique = true)]
)
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null
)
