package com.laul.lauwell.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.BuildConfig
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricRegistry
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.settings.ThemeMode
import com.laul.lauwell.core.ui.components.FilledButton
import com.laul.lauwell.core.ui.components.IconToggleOption
import com.laul.lauwell.core.ui.components.LauWellBottomSheet
import com.laul.lauwell.core.ui.components.LinkRow
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.PanelDivider
import com.laul.lauwell.core.ui.components.QuietButton
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.Section
import com.laul.lauwell.core.ui.components.SegmentedControl
import com.laul.lauwell.core.ui.components.TopBar
import com.laul.lauwell.core.ui.theme.LauWellTheme
import java.time.Clock

/**
 * Settings (design 20b), pushed from the gear in every tab's header; no bottom bar, back returns to
 * the tab. Groups: Appearance · App Settings · Data sources · Account. Rows whose screen doesn't exist yet are
 * drawn disabled (Notion: *Settings sub-screens*), with what is true today as their detail line.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenHealthConnect: () -> Unit) {
    val viewModel = appViewModel {
        SettingsViewModel(
            authRepository, displayPreferences, toggleRepository, healthDataSource, summaryStore, entrySync, entrySyncSchedule,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Settings", onBack = onBack, modifier = Modifier.padding(horizontal = LauWellTheme.spacing.xs))
        ScreenColumn {
            Section("Appearance") {
                state.themeMode?.let { mode ->
                    SegmentedControl(
                        options = ThemeOptions,
                        selectedIndex = ThemeMode.entries.indexOf(mode),
                        onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
                    )
                }
            }
            Section("App Settings") {
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
            Section("Data sources") {
                Panel {
                    LinkRow(
                        icon = R.drawable.ic_sync,
                        label = "Health Connect",
                        detail = healthConnectDetail(state.healthConnect, state.lastSyncedAt, Clock.systemDefaultZone()),
                        onClick = onOpenHealthConnect,
                    )
                    PanelDivider()
                    LinkRow(
                        icon = R.drawable.ic_sync,
                        label = "LauWell sync",
                        detail = entrySyncDetail(state.entries, Clock.systemDefaultZone(), state.syncActivity),
                        onClick = viewModel::syncNow,
                        actionIcon = R.drawable.ic_sync,
                        actionLabel = "Sync now",
                    )
                    PanelDivider()
                    LinkRow(R.drawable.ic_add, "Add a source", "More sources later", onClick = {}, enabled = false)
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
    SignOutWarning(unsent = state.unsentOnSignOut, onStay = viewModel::staySignedIn, onSignOut = viewModel::signOutAnyway)
}

/**
 * Asks before signing out loses [unsent] changes that haven't reached your account. Staying is the main
 * action: the changes are sent as soon as the phone is online.
 */
@Composable
private fun SignOutWarning(unsent: Int?, onStay: () -> Unit, onSignOut: () -> Unit) {
    // The count shown, kept while the sheet slides away.
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(unsent) { if (unsent != null) shown = unsent }
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    LauWellBottomSheet(visible = unsent != null, onDismissRequest = onStay) {
        Column(verticalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.xxs)) {
            BasicText("Sign out?", style = type.headline.copy(color = colors.ink))
            BasicText(
                "${if (shown == 1) "1 change hasn't" else "$shown changes haven't"} reached your account yet. " +
                    "Signing out now deletes ${if (shown == 1) "it" else "them"} from this phone.",
                style = type.body.copy(color = colors.ink2),
            )
        }
        Column {
            FilledButton(text = "Stay signed in", onClick = onStay)
            QuietButton(text = "Sign out anyway", onClick = onSignOut)
        }
    }
}

/** Same order as [ThemeMode.entries]. */
private val ThemeOptions = listOf(
    IconToggleOption(R.drawable.ic_light_mode, "Light"),
    IconToggleOption(R.drawable.ic_dark_mode, "Dark"),
    IconToggleOption(R.drawable.ic_brightness_auto, "System"),
)
