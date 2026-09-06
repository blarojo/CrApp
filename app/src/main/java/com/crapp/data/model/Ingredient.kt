package com.crapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical ingredient catalog, deduped across foods (docs/future-features.md spec
 * 9) -- e.g. "coconut oil" appears once here even though several [Food] rows list
 * it. [name] is stored lowercase/trimmed by [com.crapp.data.db.IngredientParser] so
 * lookups are consistent regardless of how a label capitalized it.
 */
@Entity(
    tableName = "ingredient",
    indices = [Index(value = ["name"], unique = true)]
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
