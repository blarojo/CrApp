package com.crapp.data.model

/**
 * Where a bowel movement happened (docs/future-features.md spec 3). [OTHER] pairs
 * with [BowelMovement.locationOther] for anything that doesn't fit the three named
 * places, rather than forcing it into one of those or losing it as free text on its
 * own. [displayName] is shown in the UI instead of the raw enum constant name so
 * [HOME] can read as "Inside home" (distinguishing it from [GARDEN]) without
 * changing the stored value -- existing rows and CSV/backup data keep meaning "HOME".
 */
enum class Location(val displayName: String) {
    HOME("Inside home"),
    GARDEN("Garden"),
    WALK("Walk"),
    OTHER("Other")
}
