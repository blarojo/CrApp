package com.crapp.ocr

/**
 * Heuristically isolates the "ingredients"/"composition" section from a food
 * label's full OCR'd text -- no AI/cloud call, just rule-based text parsing on the
 * client (matches the OCR model itself: on-device, no network). A follow-up to
 * docs/backlog.md spec 12 after real-world use showed the raw recognized text
 * (nutritional info, weight, address, etc. included) needed manual trimming before
 * it was usable as-is.
 *
 * Most pet-food labels (and human food labels) have a clearly labeled section --
 * **"Ingredients"** (human food, common on US pet food) or **"Composition"** (UK/EU
 * pet food -- the convention this app's own seed data's real labels use, see
 * `SeedFoods.kt`) -- followed by a comma-separated list, ending where the next
 * section starts (analytical constituents, nutritional info, additives, feeding
 * guide, best-before, etc.). This finds that start marker and cuts the text off at
 * the next recognized section marker.
 *
 * This is an approximation, not a guarantee -- OCR line breaks don't always land
 * where a real paragraph break would, and an unusual label layout or wording won't
 * match either marker list. That's an acceptable trade-off here specifically
 * because the result is only ever a *pre-fill*: the ingredients field stays fully
 * editable and nothing is auto-saved unreviewed (see docs/backlog.md spec 12).
 */
object IngredientsTextExtractor {

    private val startMarkers = listOf("ingredients", "composition")

    // Common next-section headings on pet/human food labels -- extraction stops at
    // the first line (after the start marker's line) containing one of these.
    private val stopMarkers = listOf(
        "analytical constituents", "analytical", "guaranteed analysis",
        "nutritional", "nutrition facts", "additives", "feeding guide",
        "feeding instructions", "best before", "best-before", "use by",
        "net weight", "storage", "batch", "manufactured", "distributed",
        "allergen", "warning", "directions for use"
    )

    /**
     * Returns the isolated ingredients text, or `null` if no recognizable
     * "Ingredients"/"Composition" heading was found -- callers should fall back to
     * the full raw OCR text in that case, so a failed extraction never loses data
     * the user could still want.
     */
    fun extract(rawText: String): String? {
        val lines = rawText.lines()
        val startLineIndex = lines.indexOfFirst { line -> containsMarker(line, startMarkers) }
        if (startLineIndex == -1) return null

        val collected = StringBuilder(textAfterMarker(lines[startLineIndex]))
        for (i in (startLineIndex + 1) until lines.size) {
            val line = lines[i]
            if (containsMarker(line, stopMarkers)) break
            if (line.isNotBlank()) {
                if (collected.isNotEmpty()) collected.append(' ')
                collected.append(line.trim())
            }
        }

        return collected.toString().trim().trim(':', '-', ' ').ifBlank { null }
    }

    private fun containsMarker(line: String, markers: List<String>): Boolean {
        val lower = line.lowercase()
        return markers.any { marker -> lower.contains(marker) }
    }

    /** The text on the start marker's own line, after the marker word and any ":"/"-" separator. */
    private fun textAfterMarker(line: String): String {
        val lower = line.lowercase()
        val marker = startMarkers.firstOrNull { lower.contains(it) } ?: return ""
        val after = line.substring(lower.indexOf(marker) + marker.length)
        return after.trimStart(':', '-', ' ').trim()
    }
}
