package com.laul.lauwell.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.feature.care.CareScreen
import com.laul.lauwell.feature.onboarding.HealthConnectGrantScreen
import com.laul.lauwell.feature.onboarding.OnboardingViewModel
import com.laul.lauwell.feature.records.RecordsScreen
import com.laul.lauwell.feature.settings.SettingsScreen
import com.laul.lauwell.feature.today.TodayScreen
import com.laul.lauwell.feature.tracker.MetricDetailScreen
import com.laul.lauwell.feature.tracker.TrackerScreen

/**
 * Root navigation: the four tabs, each a nested graph, plus Settings. The one place that knows
 * every screen. Screens handle no window insets themselves: this host pads for the status bar,
 * and the bottom bar (or the host, when the bar is hidden) for the navigation bar.
 */
@Composable
fun LauWellNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = TopLevelTab.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.route == tab.graphRoute } == true
    }
    val showBar = selectedTab != null

    // Once after sign-in (design: sign-in → Health Connect grant → Today), until answered.
    val onboardingViewModel = appViewModel { OnboardingViewModel(onboarding) }
    val promptSeen by onboardingViewModel.healthConnectPromptSeen.collectAsStateWithLifecycle()
    LaunchedEffect(promptSeen) {
        if (promptSeen == false) navController.navigate(Routes.HEALTH_CONNECT) { launchSingleTop = true }
    }

    Column(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = TopLevelTab.Today.graphRoute,
            modifier = Modifier
                .weight(1f)
                .statusBarsPadding()
                .then(if (showBar) Modifier else Modifier.navigationBarsPadding()),
        ) {
            navigation(route = TopLevelTab.Today.graphRoute, startDestination = TopLevelTab.Today.route) {
                composable(TopLevelTab.Today.route) {
                    TodayScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
                }
            }
            navigation(route = TopLevelTab.Tracker.graphRoute, startDestination = TopLevelTab.Tracker.route) {
                composable(TopLevelTab.Tracker.route) {
                    TrackerScreen(
                        onOpenMetric = { navController.navigate(Routes.metricDetail(it)) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(
                    Routes.METRIC_DETAIL,
                    arguments = listOf(navArgument(Routes.METRIC_ID_ARG) { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString(Routes.METRIC_ID_ARG).orEmpty()
                    MetricDetailScreen(metricId = id, onBack = navController::popBackStack)
                }
            }
            navigation(route = TopLevelTab.Care.graphRoute, startDestination = TopLevelTab.Care.route) {
                composable(TopLevelTab.Care.route) { CareScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) }) }
            }
            navigation(route = TopLevelTab.Records.graphRoute, startDestination = TopLevelTab.Records.route) {
                composable(TopLevelTab.Records.route) { RecordsScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) }) }
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = navController::popBackStack,
                    onOpenHealthConnect = { navController.navigate(Routes.HEALTH_CONNECT) },
                )
            }
            composable(Routes.HEALTH_CONNECT) { HealthConnectGrantScreen(onDone = navController::popBackStack) }
        }
        if (showBar) {
            BottomNavBar(
                selected = selectedTab,
                onSelect = { tab ->
                    navController.navigate(tab.graphRoute) {
                        // Standard bottom-nav behaviour: one copy of each tab, each keeping its own stack.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
