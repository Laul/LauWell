package com.laul.lauwell.core.navigation

import androidx.navigation.NavController

/**
 * Switches to [tab] from the bottom bar or the rail, always on the tab's main screen, even when that
 * tab is the current one (e.g. on a metric's detail, Tracker's icon returns to the Tracker list).
 *
 * The back stack is Today, then at most one other tab on top: Back from any tab returns to Today.
 * Tabs don't remember an open detail (no saved state), so a detail opened from Today never comes
 * back in Today's place.
 */
internal fun NavController.selectTab(tab: TopLevelTab) {
    popBackStack(TopLevelTab.Today.route, inclusive = false)
    if (tab != TopLevelTab.Today) navigate(tab.graphRoute)
}
