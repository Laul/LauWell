package com.laul.lauwell.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.ui.chart.MetricChart
import com.laul.lauwell.ui.format
import com.laul.lauwell.ui.formatTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailScreen(state: MetricDetailUiState, zone: ZoneId, onBack: () -> Unit) {
    val metric = state.metric
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(metric.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.latest?.let {
                Column {
                    Text("Last measure · ${formatTime(it.end, zone)}", style = MaterialTheme.typography.bodySmall)
                    Text("${metric.format(it.value)} ${metric.unit}", style = MaterialTheme.typography.displaySmall)
                }
            }
            state.stats?.let { stats ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat(if (metric == Metric.STEPS) "Daily average" else "Average", metric.format(stats.average))
                    Stat("Min", metric.format(stats.min))
                    Stat("Max", metric.format(stats.max))
                }
            }
            state.daily?.let {
                Text("Last 7 days", style = MaterialTheme.typography.titleSmall)
                MetricChart(it, modifier = Modifier.fillMaxWidth().height(200.dp))
            }
            state.timeline?.let {
                Text("Every reading", style = MaterialTheme.typography.titleSmall)
                MetricChart(it, modifier = Modifier.fillMaxWidth().height(220.dp))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
