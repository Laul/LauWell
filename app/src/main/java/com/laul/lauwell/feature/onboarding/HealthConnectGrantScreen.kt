package com.laul.lauwell.feature.onboarding

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.R
import com.laul.lauwell.core.catalog.MetricId
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.healthconnect.HealthConnectAvailability
import com.laul.lauwell.core.ui.components.FilledButton
import com.laul.lauwell.core.ui.components.LauWellIcon
import com.laul.lauwell.core.ui.components.MetricIconTile
import com.laul.lauwell.core.ui.components.Panel
import com.laul.lauwell.core.ui.components.PanelDivider
import com.laul.lauwell.core.ui.components.QuietButton
import com.laul.lauwell.core.ui.components.Switch
import com.laul.lauwell.core.ui.components.TopBar
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * "Connect Health Connect" as designed: opened once after sign-in, and from Settings. Each row is a
 * visible metric read from Health Connect, on by default; one already allowed says so (taking a
 * permission back happens in Health Connect's own settings). [onDone] after the prompt or "Not now".
 */
@Composable
fun HealthConnectGrantScreen(onDone: () -> Unit) {
    val viewModel = appViewModel { HealthConnectGrantViewModel(toggleRepository, healthDataSource, onboarding, backgroundSync) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val requestPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.onPermissionsResult()
        onDone()
    }
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val spacing = LauWellTheme.spacing

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "", onBack = { viewModel.notNow(); onDone() }, modifier = Modifier.padding(horizontal = spacing.xs))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.xxs, bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                BasicText("Connect Health Connect", style = type.headline.copy(color = colors.ink))
                BasicText(
                    "LauWell reads what your watch and phone already record. It only asks for the metrics you " +
                        "track, and you can change this any time.",
                    style = type.body.copy(color = colors.ink2),
                )
            }
            when (state.availability) {
                null -> Unit
                HealthConnectAvailability.Available -> {
                    GrantRows(state, viewModel::toggle)
                    if (state.historyToAsk) {
                        BasicText(
                            "Health Connect will also ask to read your past data, so charts can go back three months " +
                                "instead of starting today.",
                            style = type.caption.copy(color = colors.ink2),
                        )
                    }
                    if (state.backgroundToAsk) {
                        BasicText(
                            "It will also ask to read in the background, so your data is up to date when you open LauWell.",
                            style = type.caption.copy(color = colors.ink2),
                        )
                    }
                }
                HealthConnectAvailability.UpdateRequired -> BasicText(
                    "Health Connect needs to be installed or updated before LauWell can read your data.",
                    style = type.body.copy(color = colors.ink),
                )
                HealthConnectAvailability.Unavailable -> BasicText(
                    "Health Connect isn't available on this phone, so LauWell can't read sensor data here. " +
                        "You can still log entries yourself.",
                    style = type.body.copy(color = colors.ink),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                LauWellIcon(R.drawable.ic_lock, contentDescription = null, tint = colors.ink2, size = 18.dp)
                BasicText(
                    "Sensor data stays on this phone. Only daily summaries are saved to your account.",
                    style = type.caption.copy(color = colors.ink2),
                )
            }
        }
        Actions(
            state = state,
            onAllow = { requestPermissions.launch(viewModel.permissionsToRequest()) },
            onNotNow = { viewModel.notNow(); onDone() },
        )
    }
}

@Composable
private fun GrantRows(state: GrantState, onToggle: (MetricId) -> Unit) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    Panel {
        state.rows.forEachIndexed { index, row ->
            if (index > 0) PanelDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .then(
                        if (row.granted) Modifier
                        else Modifier.toggleable(value = row.selected, role = Role.Switch, onValueChange = { onToggle(row.metric.id) }),
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm),
            ) {
                MetricIconTile(row.metric.icon, row.metric.category)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BasicText(row.metric.label, style = type.item.copy(color = colors.ink))
                    BasicText(
                        "${row.metric.category.label} · " + if (row.granted) "allowed" else "read only",
                        style = type.caption.copy(color = colors.ink2),
                    )
                }
                if (!row.granted) Switch(checked = row.selected)
            }
        }
    }
}

@Composable
private fun ColumnScope.Actions(state: GrantState, onAllow: () -> Unit, onNotNow: () -> Unit) {
    val context = LocalContext.current
    val spacing = LauWellTheme.spacing
    Column(
        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        when (state.availability) {
            HealthConnectAvailability.Available -> {
                val count = state.toAsk.size
                when {
                    count > 0 -> FilledButton(if (count == 1) "Allow 1 permission" else "Allow $count permissions", onClick = onAllow)
                    // Every metric allowed, only past data and/or background reads are left: ask on their own.
                    state.historyToAsk && state.backgroundToAsk -> FilledButton("Allow 2 permissions", onClick = onAllow)
                    state.historyToAsk -> FilledButton("Allow past data", onClick = onAllow)
                    state.backgroundToAsk -> FilledButton("Allow background updates", onClick = onAllow)
                    else -> FilledButton("Done", onClick = onNotNow)
                }
            }
            HealthConnectAvailability.UpdateRequired -> FilledButton(
                text = "Get Health Connect",
                onClick = { context.openHealthConnectInPlayStore() },
            )
            else -> Unit
        }
        val nothingToAsk = state.availability == HealthConnectAvailability.Available && state.toAsk.isEmpty() &&
            !state.historyToAsk && !state.backgroundToAsk
        if (!nothingToAsk) QuietButton(text = "Not now", onClick = onNotNow, modifier = Modifier.fillMaxWidth())
    }
}

/** Health Connect's Play Store page, which installs or updates it (Android 13 and below). */
private fun Context.openHealthConnectInPlayStore() {
    val uri = "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding".toUri()
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending"))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri()))
    }
}

