package com.crapp.data.db

/**
 * Splits a [com.crapp.data.model.Food.ingredients] free-text label into canonical
 * ingredient names (docs/future-features.md spec 9). Used both for the one-time
 * backfill of the 4 seeded foods ([IngredientBackfill]) and for any food added later
 * (manual entry today, OCR capture in a future spec) -- the same parsing step
 * applies either way.
 *
 * Canonicalization is the hard part flagged in the spec: labels disagree on naming
 * for the same real ingredient (e.g. Hill's own two products list "Maize starch" vs
 * "Corn starch"). [SYNONYMS] below is a *known, not exhaustive* alias map built from
 * the 4 currently-seeded labels -- review/extend it before trusting any allergy
 * inference built on top of this data with a 5th+ food.
 */
object IngredientParser {

    /** Strips a trailing parenthetical annotation, e.g. "chicken liver hydrolysate (26%)" -> "chicken liver hydrolysate". */
    private val parentheticalRegex = Regex("""\s*\([^)]*\)""")

    /**
     * Same real ingredient under different label wording, observed across the 4
     * seeded foods. Keys and values are already lowercase/trimmed (the form
     * [canonicalize] compares against). Generic catch-alls ("vitamins and trace
     * elements" / "vitamins and minerals") are folded into one bucket rather than
     * dropped -- not useful for allergy correlation either way, but keeping the row
     * loses less than silently discarding label content.
     */
    private val synonyms: Map<String, String> = mapOf(
        "corn starch" to "maize starch",
        "linseed" to "flaxseed",
        "chicken liver hydrolysate" to "hydrolyzed chicken liver",
        "ground pecan nut shells" to "ground pecan shells",
        "powdered cellulose" to "cellulose",
        "soya bean oil" to "soybean oil",
        "soyabean oil" to "soybean oil",
        "soya oil" to "soybean oil",
        "vitamins and minerals" to "vitamins and trace elements"
    )

    /** Returns the canonical ingredient names in [ingredientsText]'s original label order, deduped. */
    fun parse(ingredientsText: String?): List<String> {
        if (ingredientsText.isNullOrBlank()) return emptyList()
        val seen = LinkedHashSet<String>()
        ingredientsText.split(",")
            .map { canonicalize(it) }
            .filter { it.isNotEmpty() }
            .forEach { seen.add(it) }
        return seen.toList()
    }

    private fun canonicalize(rawToken: String): String {
        val withoutAnnotation = rawToken.replace(parentheticalRegex, "").trim()
        // The label's final ingredient carries the sentence-ending "." (every one of
        // the 4 seeded labels ends "..., vitamins and trace elements." or similar) --
        // caught via on-device testing, where it silently defeated the
        // "vitamins and minerals" -> "vitamins and trace elements" synonym below
        // (the raw token was "vitamins and minerals.", which never matched the
        // period-less key), leaving two near-identical rows instead of one.
        val withoutTrailingPeriod = withoutAnnotation.removeSuffix(".").trim()
        val lower = withoutTrailingPeriod.lowercase()
        return synonyms[lower] ?: lower
    }
}
