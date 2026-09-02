package com.crapp.ui.nav

/** Central registry of navigation destinations, keyed by simple string routes. */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"

    const val LOG_BOWEL_MOVEMENT_PATTERN = "log_bowel_movement?id={id}"
    const val LOG_FOOD_PATTERN = "log_food?id={id}"
    const val LOG_MEDICATION_PATTERN = "log_medication?id={id}"

    /** Pass [id] to open a Log screen pre-populated for editing an existing entry. */
    fun logBowelMovement(id: Long = -1L) = "log_bowel_movement?id=$id"
    fun logFood(id: Long = -1L) = "log_food?id=$id"
    fun logMedication(id: Long = -1L) = "log_medication?id=$id"
}
