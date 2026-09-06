package com.crapp.data.repository

import com.crapp.data.db.FoodDao
import com.crapp.data.db.IngredientDao
import com.crapp.data.db.IngredientParser
import com.crapp.data.model.Food
import com.crapp.data.model.FoodIngredient
import com.crapp.data.model.Ingredient
import kotlinx.coroutines.flow.first

/**
 * Structures each [Food]'s free-text `ingredients` label into [Ingredient] +
 * [FoodIngredient] rows (docs/future-features.md spec 9) so a future insights
 * feature can query per-ingredient rather than string-match the label text.
 */
class IngredientRepository(
    private val ingredientDao: IngredientDao,
    private val foodDao: FoodDao
) {
    fun observeIngredientsForFood(foodId: Long) = ingredientDao.observeIngredientsForFood(foodId)

    /**
     * One-time backfill (not a migration): parses every [Food] row's `ingredients`
     * text into structured rows. Safe to call repeatedly -- a no-op once
     * [food_ingredient] is already populated for a food, so it also covers foods
     * added after the initial backfill (e.g. a new catalog entry) without
     * re-processing everything each time. Called once at app startup; see
     * [com.crapp.CrAppApplication].
     */
    suspend fun backfillIfNeeded() {
        val foods = foodDao.observeAll().first()
        val alreadyStructured = ingredientDao.getAllFoodIngredients().map { it.foodId }.toSet()
        for (food in foods) {
            if (food.id in alreadyStructured) continue
            structureIngredientsFor(food)
        }
    }

    /** (Re)structures one food's ingredients -- e.g. after editing its `ingredients` text in the Food Catalog. */
    suspend fun structureIngredientsFor(food: Food) {
        ingredientDao.deleteFoodIngredientsForFood(food.id)
        val names = IngredientParser.parse(food.ingredients)
        names.forEachIndexed { position, name ->
            val ingredientId = ingredientDao.getByName(name)?.id
                ?: ingredientDao.insert(Ingredient(name = name)).let { insertedId ->
                    if (insertedId != -1L) insertedId else ingredientDao.getByName(name)?.id
                        ?: error("Failed to get or create ingredient '$name'")
                }
            ingredientDao.insertFoodIngredient(
                FoodIngredient(foodId = food.id, ingredientId = ingredientId, position = position)
            )
        }
    }
}
