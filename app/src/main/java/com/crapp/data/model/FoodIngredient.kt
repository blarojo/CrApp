package com.crapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Join row: one [Ingredient] appearing in one [Food]'s label (docs/future-features.md
 * spec 9). [position] preserves the label's original order -- pet-food labels list
 * ingredients by descending concentration, so that ordering carries real meaning for
 * an allergy read, not just display.
 */
@Entity(
    tableName = "food_ingredient",
    foreignKeys = [
        ForeignKey(entity = Food::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Ingredient::class, parentColumns = ["id"], childColumns = ["ingredientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("foodId"), Index("ingredientId")]
)
data class FoodIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long,
    val ingredientId: Long,
    val position: Int
)
