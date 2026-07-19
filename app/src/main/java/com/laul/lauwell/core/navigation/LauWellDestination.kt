package com.laul.lauwell.core.navigation

/**
 * All top-level destinations in the app. One entry per feature module plus the cross-cutting
 * Home/Settings screens. A feature not yet "enabled" by the user (see Settings module, PLAN.md)
 * simply isn't shown in navigation — it doesn't need to be removed from this list.
 */
enum class LauWellDestination(val route: String, val label: String) {
    Home(route = "home", label = "Home"),
    Vitals(route = "vitals", label = "Vitals"),
    Activity(route = "activity", label = "Activity"),
    Sleep(route = "sleep", label = "Sleep"),
    Medication(route = "medication", label = "Medication"),
    Ostomy(route = "ostomy", label = "Ostomy"),
    Settings(route = "settings", label = "Settings"),
}
