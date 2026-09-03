package com.crapp.data.model

/**
 * How much was passed in a single bowel movement (docs/future-features.md spec 1) --
 * a coarse 3-point read, independent of [BowelMovement.consistency] (a small amount
 * can still be liquid, and vice versa), so kept as its own field rather than derived.
 */
enum class Amount(val displayName: String) {
    SOME_DRIPS("Some drips"),
    MEDIUM_AMOUNT("Medium amount"),
    A_LOT("A lot of poo")
}
