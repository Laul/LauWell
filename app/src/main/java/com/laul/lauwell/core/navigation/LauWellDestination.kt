package com.laul.lauwell.core.navigation

/**
 * All top-level destinations in the app. One entry per feature module plus the cross-cutting
 * Home/Settings screens. A feature not yet "enabled" by the user (see Settings module, PLAN.md)
 * simply isn't shown in navigation — it doesn't need to be removed from this list.
 *
 * Data-type feature modules (health, medication, ostomy, glycemia, ...) are intentionally not
 * wired in yet — their package layout is still being decided, see PLAN.md. Add entries here as
 * each one lands.
 */
enum class LauWellDestination(val route: String, val label: String) {
    Home(route = "home", label = "Home"),
    Settings(route = "settings", label = "Settings"),
}
