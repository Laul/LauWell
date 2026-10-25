package com.laul.lauwell.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.BuildConfig
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.settings.ThemeMode
import com.laul.lauwell.core.ui.components.IconToggleOption
import com.laul.lauwell.core.ui.components.LinkRow
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.PanelDivider
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.Section
import com.laul.lauwell.core.ui.components.SegmentedControl
import com.laul.lauwell.core.ui.components.TopBar
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.Clock

/**
 * Settings (design 20b), pushed from the gear in every tab's header; no bottom bar, back returns to
 * the tab. Groups: Data sources · Appearance · App · Account. Rows whose screen doesn't exist yet are
 * drawn disabled (Notion: *Settings sub-screens*), with what is true today as their detail line.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenHealthConnect: () -> Unit) {
    val viewModel = appViewModel {
        SettingsViewModel(authRepository, displayPreferences, toggleRepository, healthDataSource, summaryStore)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Settings", onBack = onBack, modifier = Modifier.padding(horizontal = LauWellTheme.spacing.xs))
        ScreenColumn {
            Section("Data sources") {
                Panel {
                    LinkRow(
                        icon = R.drawable.ic_sync,
                        label = "Health Connect",
                        detail = healthConnectDetail(state.healthConnect, state.lastSyncedAt, Clock.systemDefaultZone()),
                        onClick = onOpenHealthConnect,
                    )
                    PanelDivider()
                    LinkRow(R.drawable.ic_add, "Add a source", "More sources later", onClick = {}, enabled = false)
                }
            }
            Section("Appearance") {
                state.themeMode?.let { mode ->
                    SegmentedControl(
                        options = ThemeOptions,
                        selectedIndex = ThemeMode.entries.indexOf(mode),
                        onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
                    )
                }
            }
            Section("App") {
                Panel {
                    LinkRow(
                        icon = R.drawable.ic_visibility,
                        label = "Metrics shown",
                        detail = "${state.visibleMetrics} of ${MetricRegistry.all.size} · per category",
                        onClick = {},
                        enabled = false,
                    )
                    PanelDivider()
                    LinkRow(R.drawable.ic_straighten, "Units", "Metric · kg, cm, mmol/L", onClick = {}, enabled = false)
                    PanelDivider()
                    LinkRow(R.drawable.ic_notifications, "Notifications", "None yet", onClick = {}, enabled = false)
                    PanelDivider()
                    LinkRow(R.drawable.ic_download, "Export & delete data", "CSV / PDF, later", onClick = {}, enabled = false)
                }
            }
            Section("Account") {
                Panel {
                    LinkRow(
                        icon = R.drawable.ic_account_circle,
                        label = "Google account",
                        detail = state.email?.let { "Signed in as $it" } ?: "Signed in",
                        onClick = null,
                    )
                    PanelDivider()
                    LinkRow(R.drawable.ic_logout, "Sign out", "Your data stays in your account", onClick = viewModel::signOut)
                    PanelDivider()
                    LinkRow(
                        icon = R.drawable.ic_info,
                        label = "About LauWell",
                        detail = "Version ${BuildConfig.VERSION_NAME} · open-source notices",
                        onClick = {},
                        enabled = false,
                    )
                }
            }
        }
    }
}

/** Same order as [ThemeMode.entries]. */
private val ThemeOptions = listOf(
    IconToggleOption(R.drawable.ic_light_mode, "Light"),
    IconToggleOption(R.drawable.ic_dark_mode, "Dark"),
    IconToggleOption(R.drawable.ic_brightness_auto, "System"),
)
