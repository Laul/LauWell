package com.laul.lauwell.ui.overview

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.ui.chart.MetricChart
import com.laul.lauwell.ui.format
import com.laul.lauwell.ui.formatTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    state: OverviewUiState,
    zone: ZoneId,
    onOpen: (Metric) -> Unit,
    onGrant: (Metric) -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("LauWell") }) }) { padding ->
        PullToRefreshBox(
            isRefreshing = state.syncing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!state.healthConnectAvailable) {
                    item { HealthConnectMissing() }
                }
                if (state.failures.isNotEmpty()) {
                    item {
                        Text(
                            "Couldn't sync: " + state.failures.joinToString { it.metric.displayName },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                items(state.cards, key = { it.metric }) { card ->
                    MetricCardView(card, zone, onOpen = { onOpen(card.metric) }, onGrant = { onGrant(card.metric) })
                }
            }
        }
    }
}

@Composable
private fun MetricCardView(card: MetricCard, zone: ZoneId, onOpen: () -> Unit, onGrant: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(card.metric.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                card.latest?.let {
                    Text(formatTime(it.end, zone), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!card.canRead) {
                Button(onClick = onGrant, modifier = Modifier.padding(top = 8.dp)) { Text("Allow access") }
                return@Column
            }
            card.latest?.let {
                Text(
                    "${card.metric.format(it.value)} ${card.metric.unit}",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            MetricChart(card.chart, compact = true, modifier = Modifier.fillMaxWidth().height(80.dp))
        }
    }
}

@Composable
private fun HealthConnectMissing() {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Health Connect is needed to read your data.", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending"))
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Install or update") }
        }
    }
}
