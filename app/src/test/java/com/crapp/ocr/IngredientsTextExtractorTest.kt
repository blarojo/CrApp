package com.crapp.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [IngredientsTextExtractor] is pure text logic (no Android/ML Kit dependency), so
 * unlike the rest of the OCR flow (see docs/app-functionality.md's Testing status --
 * a real camera capture can't be automated), this part is fully unit-testable.
 * Fixtures below mimic realistic OCR line-breaks on a UK/EU pet-food label (this
 * app's own seed data's real labels use "Composition" -- see `SeedFoods.kt`) and a
 * US-style human-food label ("Ingredients").
 */
class IngredientsTextExtractorTest {

    @Test
    fun extract_ukPetFoodLabel_stopsAtAnalyticalConstituents() {
        val ocrText = """
            Hill's Prescription Diet
            z/d Mini Food Sensitivities
            Composition: Maize starch, chicken liver
            hydrolysate (26%), ground pecan nut shells,
            cellulose, coconut oil, minerals, linseed
            (flaxseed), dried beet pulp, vitamins and
            trace elements.
            Analytical constituents: Protein 18%, Fat 12%
            Best before: see cap
        """.trimIndent()

        val result = IngredientsTextExtractor.extract(ocrText)

        assertEquals(
            "Maize starch, chicken liver hydrolysate (26%), ground pecan nut shells, " +
                "cellulose, coconut oil, minerals, linseed (flaxseed), dried beet pulp, " +
                "vitamins and trace elements.",
            result
        )
    }

    @Test
    fun extract_usHumanFoodLabel_ingredientsHeading_stopsAtNutritionFacts() {
        val ocrText = """
            Best Brand Peanut Butter
            Net Wt 16 oz (454g)
            INGREDIENTS: Roasted peanuts, sugar, palm oil, salt.
            Nutrition Facts
            Serving size 2 tbsp
        """.trimIndent()

        val result = IngredientsTextExtractor.extract(ocrText)

        assertEquals("Roasted peanuts, sugar, palm oil, salt.", result)
    }

    @Test
    fun extract_caseInsensitiveMarkers() {
        val ocrText = "composition: Chicken, rice, water.\nADDITIVES: E330"

        val result = IngredientsTextExtractor.extract(ocrText)

        assertEquals("Chicken, rice, water.", result)
    }

    @Test
    fun extract_noStopMarkerFound_takesEverythingAfterTheHeading() {
        val ocrText = "Ingredients: Water, salt, pepper.\nEnjoy!"

        val result = IngredientsTextExtractor.extract(ocrText)

        assertEquals("Water, salt, pepper. Enjoy!", result)
    }

    @Test
    fun extract_noHeadingFound_returnsNullSoCallerCanFallBackToFullText() {
        val ocrText = "Best Brand Dog Treats\nNet weight 200g\nBest before 2027"

        val result = IngredientsTextExtractor.extract(ocrText)

        assertNull(result)
    }

    @Test
    fun extract_blankAfterHeading_returnsNull() {
        val ocrText = "Composition:\n\nAnalytical constituents: Protein 10%"

        val result = IngredientsTextExtractor.extract(ocrText)

        assertNull(result)
    }
}
