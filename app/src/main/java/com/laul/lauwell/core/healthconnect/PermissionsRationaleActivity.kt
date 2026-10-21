package com.laul.lauwell.core.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.ui.components.ScreenColumn
import com.laul.lauwell.core.ui.components.TopBar
import com.laul.lauwell.core.ui.theme.LauWellTheme

/**
 * What LauWell does with Health Connect data. Health Connect opens it from its permission screen
 * ("privacy policy") and refuses to show the grant prompt to an app without one: the manifest
 * registers it for Android 13 and below (`ACTION_SHOW_PERMISSIONS_RATIONALE`) and, through an
 * activity-alias, for Android 14 and up (`VIEW_PERMISSION_USAGE` + `HEALTH_PERMISSIONS`).
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LauWellTheme {
                val colors = LauWellTheme.colors
                val body = LauWellTheme.typography.body.copy(color = colors.ink)
                Column(Modifier.fillMaxSize().background(colors.bg).safeDrawingPadding()) {
                    TopBar(
                        title = "Your health data",
                        onBack = ::finish,
                        modifier = Modifier.padding(horizontal = LauWellTheme.spacing.xs),
                    )
                    ScreenColumn {
                        BasicText(
                            "LauWell reads the data you allow from Health Connect, such as heart rate and blood " +
                                "pressure, to show it on Today and Tracker. It never writes to Health Connect.",
                            style = body,
                        )
                        BasicText(
                            "Sensor data stays on this phone. Only daily summaries are saved to your account.",
                            style = body,
                        )
                        BasicText(
                            "You can change what LauWell reads at any time in Health Connect's settings.",
                            style = body.copy(color = colors.ink2),
                        )
                    }
                }
            }
        }
    }
}
