package com.laul.lauwell.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.laul.lauwell.feature.activity.ActivityScreen
import com.laul.lauwell.feature.home.HomeScreen
import com.laul.lauwell.feature.medication.MedicationScreen
import com.laul.lauwell.feature.ostomy.OstomyScreen
import com.laul.lauwell.feature.settings.SettingsScreen
import com.laul.lauwell.feature.sleep.SleepScreen
import com.laul.lauwell.feature.vitals.VitalsScreen

/**
 * Root navigation graph. Wires every feature module's top-level screen into one NavHost.
 *
 * Each feature module stays self-contained (own package, own internal screens/state) and only
 * exposes a single entry-point composable here, keeping this file as the one place that knows
 * about all modules at once.
 */
@Composable
fun LauWellNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = LauWellDestination.Home.route) {
        composable(LauWellDestination.Home.route) { HomeScreen() }
        composable(LauWellDestination.Vitals.route) { VitalsScreen() }
        composable(LauWellDestination.Activity.route) { ActivityScreen() }
        composable(LauWellDestination.Sleep.route) { SleepScreen() }
        composable(LauWellDestination.Medication.route) { MedicationScreen() }
        composable(LauWellDestination.Ostomy.route) { OstomyScreen() }
        composable(LauWellDestination.Settings.route) { SettingsScreen() }
    }
}
