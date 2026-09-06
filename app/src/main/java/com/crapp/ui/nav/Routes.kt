package com.crapp.ui.nav

/** Central registry of navigation destinations, keyed by simple string routes. */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val EXPORT = "export"
    const val SETTINGS = "settings"
    const val FOOD_CATALOG = "food_catalog"
    const val MEDICATION_CATALOG = "medication_catalog"
    const val INSIGHTS = "insights"

    const val LOG_BOWEL_MOVEMENT_PATTERN = "log_bowel_movement?id={id}"
    const val LOG_FOOD_PATTERN = "log_food?id={id}"
    const val LOG_MEDICATION_PATTERN = "log_medication?id={id}"
    const val LOG_ENERGY_PATTERN = "log_energy?id={id}"
    const val LOG_WALK_PATTERN = "log_walk?id={id}"

    /** Pass [id] to open a Log screen pre-populated for editing an existing entry. */
    fun logBowelMovement(id: Long = -1L) = "log_bowel_movement?id=$id"
    fun logFood(id: Long = -1L) = "log_food?id=$id"
    fun logMedication(id: Long = -1L) = "log_medication?id=$id"
    fun logEnergy(id: Long = -1L) = "log_energy?id=$id"
    fun logWalk(id: Long = -1L) = "log_walk?id=$id"
}
