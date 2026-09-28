package com.laul.lauwell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.laul.lauwell.ui.theme.LauWellTheme

/** Explains what LauWell reads from Health Connect and why. Required for Play review. */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauWellTheme {
                Column(Modifier.padding(24.dp)) {
                    Text("How LauWell uses your health data", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "LauWell reads heart rate, glucose, steps and blood pressure to show your trends. " +
                            "Data stays on this device. LauWell never writes to Health Connect.",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
