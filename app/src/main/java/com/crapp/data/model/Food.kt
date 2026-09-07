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
    val brand: String? = null,
    /**
     * Free-text ingredient list (see docs/development-plan.md Phase 8), entered
     * manually or copied from a label -- lets a food's ingredients be cross-checked
     * against logged bowel movements for potential triggers.
     */
    val ingredients: String? = null,
    /**
     * The portion this food is usually served in, e.g. 1 + "tin (400g)" for a wet
     * food, 1 + "cup" for a dry food, 50 + "g" for treats -- a follow-up to
     * docs/backlog.md spec 14's own open question ("should the quick-amount buttons
     * eventually vary by selected food? ... revisit if usage shows a food-specific
     * need" -- this is that revisit, per-food rather than two fixed global buttons).
     * When set, selecting this food on the food-logging screen pre-fills
     * [FoodEntry.amount]/[FoodEntry.amountValue]/
     * [FoodEntry.amountUnit] from it (still fully overridable before saving, same
     * "pre-fill, not lock" principle as the fixed quick-amount buttons). `null`
     * means this food has no usual amount set -- selecting it leaves the amount
     * fields as they were, it doesn't clear them.
     */
    val usualAmountValue: Double? = null,
    val usualAmountUnit: String? = null
)
