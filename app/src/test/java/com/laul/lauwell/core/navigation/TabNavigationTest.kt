package com.laul.lauwell.core.navigation

import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laul.lauwell.core.catalog.MetricId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// The tab moves on a NavController with LauWellNavHost's graph shape (screens left empty).
@RunWith(AndroidJUnit4::class)
class TabNavigationTest {

    private val nav = NavHostController(ApplicationProvider.getApplicationContext()).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        graph = createGraph(startDestination = TopLevelTab.Today.graphRoute) {
            navigation(route = TopLevelTab.Today.graphRoute, startDestination = TopLevelTab.Today.route) {
                composable(TopLevelTab.Today.route) {}
            }
            navigation(route = TopLevelTab.Tracker.graphRoute, startDestination = TopLevelTab.Tracker.route) {
                composable(TopLevelTab.Tracker.route) {}
                composable(Routes.METRIC_DETAIL) {}
            }
            navigation(route = TopLevelTab.Care.graphRoute, startDestination = TopLevelTab.Care.route) {
                composable(TopLevelTab.Care.route) {}
            }
            navigation(route = TopLevelTab.Records.graphRoute, startDestination = TopLevelTab.Records.route) {
                composable(TopLevelTab.Records.route) {}
            }
        }
    }

    private fun at() = nav.currentDestination?.route

    @Test
    fun `Today's tab returns to Today after a metric opened from Today, even via another tab`() {
        nav.navigate(Routes.metricDetail(MetricId("steps")))
        nav.popBackStack()
        nav.navigate(Routes.metricDetail(MetricId("steps")))

        nav.selectTab(TopLevelTab.Today)
        assertEquals(TopLevelTab.Today.route, at())

        nav.navigate(Routes.metricDetail(MetricId("steps")))
        nav.selectTab(TopLevelTab.Care)
        assertEquals(TopLevelTab.Care.route, at())
        nav.selectTab(TopLevelTab.Today)
        assertEquals(TopLevelTab.Today.route, at())
    }

    @Test
    fun `a metric opened from Today goes back to Today`() {
        nav.navigate(Routes.metricDetail(MetricId("steps")))
        nav.popBackStack()
        assertEquals(TopLevelTab.Today.route, at())
    }

    @Test
    fun `a tab's icon always opens its main screen`() {
        nav.navigate(Routes.metricDetail(MetricId("steps"))) // from Today: Tracker is highlighted
        nav.selectTab(TopLevelTab.Tracker)
        assertEquals(TopLevelTab.Tracker.route, at())
        nav.popBackStack()
        assertEquals(TopLevelTab.Today.route, at())

        nav.selectTab(TopLevelTab.Tracker)
        nav.navigate(Routes.metricDetail(MetricId("steps")))
        nav.selectTab(TopLevelTab.Care)
        nav.selectTab(TopLevelTab.Tracker)
        assertEquals(TopLevelTab.Tracker.route, at())
    }

    @Test
    fun `Back from any tab returns to Today`() {
        nav.selectTab(TopLevelTab.Care)
        nav.selectTab(TopLevelTab.Records)
        nav.popBackStack()
        assertEquals(TopLevelTab.Today.route, at())
    }

}
