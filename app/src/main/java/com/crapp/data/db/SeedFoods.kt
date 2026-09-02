package com.crapp.data.db

/**
 * Starter food catalog for a brand-new install (docs/development-plan.md Phase 8) --
 * inserted once by [AppDatabase]'s `RoomDatabase.Callback.onCreate`, which only
 * fires when the database file is created fresh, so it never touches an existing
 * install's data. Ingredients are each product's own listed composition as found
 * online as of Sept 2026 (manufacturers revise formulations over time -- verify
 * against the physical packaging if it matters for a specific health decision).
 */
internal data class SeedFood(val name: String, val brand: String, val ingredients: String)

internal val SEED_FOODS = listOf(
    SeedFood(
        name = "z/d Mini Food Sensitivities (Original, dry)",
        brand = "Hill's Prescription Diet",
        ingredients = "Maize starch, chicken liver hydrolysate (26%), ground pecan nut " +
            "shells, cellulose, coconut oil, minerals, linseed (flaxseed), dried beet pulp, " +
            "soya bean oil, dried citrus pulp, fish oil (0.9%), dried cranberries, vitamins " +
            "and trace elements."
    ),
    SeedFood(
        name = "z/d Food Sensitivities (Original, wet)",
        brand = "Hill's Prescription Diet",
        ingredients = "Corn starch, hydrolyzed chicken liver, hydrolyzed chicken, ground " +
            "pecan shells, powdered cellulose, flaxseed, dried beet pulp, hydrolyzed chicken " +
            "flavor, soybean oil, dried citrus pulp, dicalcium phosphate, lactic acid, " +
            "coconut oil, fish oil, calcium carbonate, pressed cranberries, glyceryl " +
            "monostearate, DL-methionine, vitamins and minerals."
    ),
    SeedFood(
        name = "HA Hypoallergenic Mousse 400g",
        brand = "Purina Pro Plan Veterinary Diets",
        ingredients = "Maize starch, hydrolysed soya protein, minerals, coconut oil, sugar, " +
            "rapeseed oil, cellulose, glycerine (vegetable origin), soya oil, fish oil, " +
            "vitamins and trace elements."
    ),
    SeedFood(
        name = "HA Hypoallergenic Dry 3kg",
        brand = "Purina Pro Plan Veterinary Diets",
        ingredients = "Corn starch, hydrolysed soya protein, minerals, coconut oil, sugar, " +
            "digest, soyabean oil, cellulose, animal fats, fish oil, monoglyceride, vitamins " +
            "and trace elements."
    )
)
