package com.laul.lauwell.core.navigation

/**
 * All top-level destinations in the app. One entry per feature module plus the cross-cutting
 * Home/Settings screens. A feature not yet "enabled" by the user (see Settings module, PLAN.md)
 * simply isn't shown in navigation — it doesn't need to be removed from this list.
 */
enum class LauWellDestination(val route: String, val label: String) {
    Home(route = "home", label = "Home"),
    Health(route = "health", label = "Health"),
    Medication(route = "medication", label = "Medication"),
    Ostomy(route = "ostomy", label = "Ostomy"),
    Glycemia(route = "glycemia", label = "Glycemia"),
    Settings(route = "settings", label = "Settings"),
}
