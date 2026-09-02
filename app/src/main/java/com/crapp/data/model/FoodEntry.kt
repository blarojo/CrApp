package com.crapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A single instance of feeding a [Food] to the dog. [foodId] points at the food
 * catalog; a food can't be deleted while entries still reference it (RESTRICT) to
 * avoid orphaning logged history.
 */
@Entity(
    tableName = "food_entry",
    foreignKeys = [
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("foodId")]
)
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val foodId: Long,
    val amount: String? = null,
    val mealType: MealType
)
