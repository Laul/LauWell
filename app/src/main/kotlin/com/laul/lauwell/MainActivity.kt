package com.laul.lauwell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.laul.lauwell.core.model.Metric
import com.laul.lauwell.ui.detail.MetricDetailScreen
import com.laul.lauwell.ui.detail.MetricDetailViewModel
import com.laul.lauwell.ui.overview.OverviewScreen
import com.laul.lauwell.ui.overview.OverviewViewModel
import com.laul.lauwell.ui.theme.LauWellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LauWellApplication).container
        setContent {
            LauWellTheme { LauWellNavHost(container) }
        }
    }
}

@Composable
private fun LauWellNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "overview") {
        composable("overview") {
            val vm: OverviewViewModel = viewModel(factory = OverviewViewModel.factory(container))
            val state by vm.state.collectAsStateWithLifecycle()
            // One permission per request, so refusing glucose doesn't block steps.
            val requestPermission = rememberLauncherForActivityResult(
                PermissionController.createRequestPermissionResultContract(),
            ) { vm.refresh() }

            OverviewScreen(
                state = state,
                zone = container.zone,
                onOpen = { metric -> nav.navigate("metric/${metric.name}") },
                onGrant = { metric ->
                    container.healthConnect?.let { requestPermission.launch(setOf(it.permissionFor(metric))) }
                },
                onRefresh = vm::refresh,
            )
        }
        composable("metric/{metric}") { entry ->
            val metric = Metric.valueOf(requireNotNull(entry.arguments?.getString("metric")))
            val vm: MetricDetailViewModel = viewModel(factory = MetricDetailViewModel.factory(metric, container))
            val state by vm.state.collectAsStateWithLifecycle()
            MetricDetailScreen(state, zone = container.zone, onBack = { nav.popBackStack() })
        }
    }
}
