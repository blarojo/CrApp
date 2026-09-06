package com.crapp.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientParserTest {

    @Test
    fun parse_splitsOnCommasAndTrims() {
        assertEquals(
            listOf("chicken", "rice", "oat fiber"),
            IngredientParser.parse("Chicken, rice,  oat fiber")
        )
    }

    @Test
    fun parse_null_or_blank_returnsEmpty() {
        assertEquals(emptyList<String>(), IngredientParser.parse(null))
        assertEquals(emptyList<String>(), IngredientParser.parse("   "))
    }

    @Test
    fun parse_stripsParentheticalPercentageAnnotations() {
        // Uses an ingredient with no synonym mapping so this test isolates the
        // parenthetical-stripping behavior alone (see the synonym-specific test below
        // for "chicken liver hydrolysate (26%)", which also canonicalizes).
        assertEquals(
            listOf("fish oil"),
            IngredientParser.parse("fish oil (0.9%)")
        )
    }

    @Test
    fun parse_canonicalizesKnownSynonymsAcrossTheFourSeededLabels() {
        // Regression coverage for the exact mismatches flagged in
        // docs/future-features.md spec 9's "canonicalization is the hard part" note --
        // without this, the same real ingredient would fragment into duplicate rows.
        assertEquals(listOf("maize starch"), IngredientParser.parse("Corn starch"))
        assertEquals(listOf("flaxseed"), IngredientParser.parse("linseed (flaxseed)"))
        assertEquals(listOf("hydrolyzed chicken liver"), IngredientParser.parse("chicken liver hydrolysate (26%)"))
        assertEquals(listOf("ground pecan shells"), IngredientParser.parse("ground pecan nut shells"))
        assertEquals(listOf("cellulose"), IngredientParser.parse("powdered cellulose"))
        assertEquals(listOf("soybean oil"), IngredientParser.parse("soya bean oil"))
        assertEquals(listOf("soybean oil"), IngredientParser.parse("soyabean oil"))
        assertEquals(listOf("soybean oil"), IngredientParser.parse("soya oil"))
        assertEquals(listOf("vitamins and trace elements"), IngredientParser.parse("vitamins and minerals"))
    }

    @Test
    fun parse_dedupesRepeatedIngredientsPreservingFirstOccurrenceOrder() {
        assertEquals(
            listOf("coconut oil", "fish oil"),
            IngredientParser.parse("coconut oil, fish oil, coconut oil")
        )
    }

    @Test
    fun parse_realSeededLabel_z_d_mini_dry_producesExpectedCanonicalNames() {
        val ingredients = "Maize starch, chicken liver hydrolysate (26%), ground pecan nut " +
            "shells, cellulose, coconut oil, minerals, linseed (flaxseed), dried beet pulp, " +
            "soya bean oil, dried citrus pulp, fish oil (0.9%), dried cranberries, vitamins " +
            "and trace elements."

        val parsed = IngredientParser.parse(ingredients)

        assertEquals(
            listOf(
                "maize starch", "hydrolyzed chicken liver", "ground pecan shells", "cellulose",
                "coconut oil", "minerals", "flaxseed", "dried beet pulp", "soybean oil",
                "dried citrus pulp", "fish oil", "dried cranberries", "vitamins and trace elements"
            ),
            parsed
        )
    }

    @Test
    fun parse_stripsTrailingPeriodOnTheLastIngredient_beforeApplyingSynonyms() {
        // Regression test: every seeded label's final ingredient carries the
        // sentence-ending period (e.g. "...vitamins and minerals.") -- found via
        // on-device testing, where it silently defeated the
        // "vitamins and minerals" -> "vitamins and trace elements" synonym below
        // because the raw token never matched the period-less synonym key.
        assertEquals(
            listOf("vitamins and trace elements"),
            IngredientParser.parse("vitamins and minerals.")
        )
        assertEquals(
            listOf("vitamins and trace elements"),
            IngredientParser.parse("vitamins and trace elements.")
        )
    }
}
