package com.laul.lauwell.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * every screen. The tabs sit in the [BottomNavBar], or in the [NavRail] on a wide window
 * ([isWideWindow]: the Fold open, or a phone on its side); there, screens keep their phone layout,
 * centred at [PhoneContentMaxWidth], until they have a wide one.
 *
 * Screens handle no window insets themselves: this host pads for the status bar and, at the sides,
 * for a navigation bar or camera cutout there; the bottom bar or the rail (or the host, without them)
 * pads for the navigation bar.
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

    val onSelect: (TopLevelTab) -> Unit = { tab ->
        navController.navigate(tab.graphRoute) {
            // Standard tab behaviour: one copy of each tab, each keeping its own stack.
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = isWideWindow(maxWidth)
        val rail = showBar && wide
        val bottomBar = showBar && !wide
        // One tree for both layouts, so the NavHost (and each screen's state) stays put when the window
        // changes width: folding or unfolding only adds or removes the rail and the bottom bar.
        Row(Modifier.fillMaxSize()) {
            if (rail) NavRail(selected = selectedTab, onSelect = onSelect)
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(if (rail) WindowInsetsSides.End else WindowInsetsSides.Horizontal),
                        )
                        .then(
                            if (bottomBar) Modifier
                            else Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = TopLevelTab.Today.graphRoute,
                        modifier = Modifier.fillMaxHeight().widthIn(max = PhoneContentMaxWidth).fillMaxWidth(),
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
                }
                if (bottomBar) BottomNavBar(selected = selectedTab, onSelect = onSelect)
            }
        }
    }
}

/** Wide enough for the [NavRail] (Android's 600 dp breakpoint): the Fold open (about 884 dp), a phone on its side. */
internal fun isWideWindow(width: Dp): Boolean = width >= 600.dp

/** How wide a screen with only a phone layout gets on a wide window, centred. */
private val PhoneContentMaxWidth = 600.dp
