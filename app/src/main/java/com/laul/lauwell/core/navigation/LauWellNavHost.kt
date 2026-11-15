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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.CompositionLocalProvider
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.ui.LocalWideWindow
import com.laul.lauwell.core.ui.PhoneContentMaxWidth
import com.laul.lauwell.core.ui.isWideWindow
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
 * ([isWideWindow]: the Fold open, or a phone on its side); there, Today shows its dashboard, Tracker its
 * week board, and the other screens keep their phone layout, centred at [PhoneContentMaxWidth], until they have a wide one.
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

    val onSelect: (TopLevelTab) -> Unit = { navController.selectTab(it) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = isWideWindow(maxWidth)
        val rail = showBar && wide
        val bottomBar = showBar && !wide
        // One tree for both layouts, so the NavHost (and each screen's state) stays put when the window
        // changes width: folding or unfolding only adds or removes the rail and the bottom bar.
        CompositionLocalProvider(LocalWideWindow provides wide) {
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
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            navigation(route = TopLevelTab.Today.graphRoute, startDestination = TopLevelTab.Today.route) {
                                composable(TopLevelTab.Today.route) {
                                    TodayScreen(
                                        // The detail belongs to Tracker (the metric's whole history): Tracker is
                                        // highlighted while it is open, and Back returns to Today.
                                        onOpenMetric = { navController.navigate(Routes.metricDetail(it)) },
                                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                    )
                                }
                            }
                            navigation(route = TopLevelTab.Tracker.graphRoute, startDestination = TopLevelTab.Tracker.route) {
                                composable(TopLevelTab.Tracker.route) {
                                    // Full width: on a wide window it is the week board.
                                    TrackerScreen(
                                        onOpenMetric = { navController.navigate(Routes.metricDetail(it)) },
                                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                    )
                                }
                                composable(
                                    Routes.METRIC_DETAIL,
                                    arguments = listOf(navArgument(Routes.METRIC_ID_ARG) { type = NavType.StringType }),
                                ) { entry ->
                                    PhoneWidth { MetricDetail(entry.arguments, onBack = navController::popBackStack) }
                                }
                            }
                            navigation(route = TopLevelTab.Care.graphRoute, startDestination = TopLevelTab.Care.route) {
                                composable(TopLevelTab.Care.route) {
                                    PhoneWidth { CareScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) }) }
                                }
                            }
                            navigation(route = TopLevelTab.Records.graphRoute, startDestination = TopLevelTab.Records.route) {
                                composable(TopLevelTab.Records.route) {
                                    PhoneWidth { RecordsScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) }) }
                                }
                            }
                            composable(Routes.SETTINGS) {
                                PhoneWidth {
                                    SettingsScreen(
                                        onBack = navController::popBackStack,
                                        onOpenHealthConnect = { navController.navigate(Routes.HEALTH_CONNECT) },
                                    )
                                }
                            }
                            composable(Routes.HEALTH_CONNECT) {
                                PhoneWidth { HealthConnectGrantScreen(onDone = navController::popBackStack) }
                            }
                        }
                    }
                    if (bottomBar) BottomNavBar(selected = selectedTab, onSelect = onSelect)
                }
            }
        }
    }
}

@Composable
private fun MetricDetail(arguments: android.os.Bundle?, onBack: () -> Unit) {
    MetricDetailScreen(metricId = arguments?.getString(Routes.METRIC_ID_ARG).orEmpty(), onBack = onBack)
}

/** A screen without a wide layout: its phone layout, centred at [PhoneContentMaxWidth] on a wide window. */
@Composable
private fun PhoneWidth(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = PhoneContentMaxWidth).fillMaxSize()) { content() }
    }
}
