package com.crapp.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedFoodsTest {

    @Test
    fun seedFoods_hasTheFourRequestedProducts() {
        assertEquals(4, SEED_FOODS.size)
        assertEquals(
            setOf(
                "z/d Mini Food Sensitivities (Original, dry)",
                "z/d Food Sensitivities (Original, wet)",
                "HA Hypoallergenic Mousse 400g",
                "HA Hypoallergenic Dry 3kg"
            ),
            SEED_FOODS.map { it.name }.toSet()
        )
    }

    @Test
    fun seedFoods_everyEntryHasABrandAndNonBlankIngredients() {
        SEED_FOODS.forEach { seed ->
            assertTrue("${seed.name} has a brand", seed.brand.isNotBlank())
            assertTrue("${seed.name} has ingredients", seed.ingredients.isNotBlank())
        }
    }

    @Test
    fun seedFoods_splitsAcrossBothRequestedBrands() {
        val brands = SEED_FOODS.map { it.brand }.toSet()
        assertEquals(setOf("Hill's Prescription Diet", "Purina Pro Plan Veterinary Diets"), brands)
    }
}
