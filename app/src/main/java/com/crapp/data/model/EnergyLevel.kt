package com.crapp.data.model

/**
 * A subjective daily energy read (docs/future-features.md spec 4) -- named rather
 * than a bare 1-5 int since a name is faster to recognize at a glance than
 * remembering what a number meant last time. Ordered low -> high so trend charts
 * can still sort/plot by [ordinal].
 */
enum class EnergyLevel(val displayName: String) {
    SLEPT_ALL_DAY("Slept all day"),
    LOW_ENERGY("Low energy"),
    NORMAL("Normal"),
    A_BIT_PLAYFUL("A bit playful"),
    A_LOT_OF_ENERGY("A lot of energy")
}
