package com.crapp.data.model

/**
 * Where a bowel movement happened (docs/future-features.md spec 3). [OTHER] pairs
 * with [BowelMovement.locationOther] for anything that doesn't fit home/walk, rather
 * than forcing it into one of those two or losing it as free text on its own.
 */
enum class Location {
    HOME,
    WALK,
    OTHER
}
