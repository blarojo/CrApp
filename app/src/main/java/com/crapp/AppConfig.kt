package com.crapp

/**
 * App is single-dog by design (see docs/development-plan.md §3) -- no `Dog` entity,
 * so the dog's name is just a display constant rather than editable data. Change it
 * here if this ever needs to track a different dog.
 */
object AppConfig {
    const val DOG_NAME = "Mango"
}
