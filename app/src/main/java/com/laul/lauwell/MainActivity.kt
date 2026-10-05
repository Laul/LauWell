package com.laul.lauwell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.auth.ui.AuthGate
import com.laul.lauwell.core.navigation.LauWellNavHost
import com.laul.lauwell.core.ui.theme.LauWellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authRepository = (application as LauWellApplication).container.authRepository
        setContent {
            LauWellTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LauWellTheme.colors.background),
                ) {
                    AuthGate(authRepository) {
                        LauWellNavHost()
                    }
                }
            }
        }
    }
}
