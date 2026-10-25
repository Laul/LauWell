package com.laul.lauwell

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.laul.lauwell.core.auth.ui.AuthGate
import com.laul.lauwell.core.navigation.LauWellNavHost
import com.laul.lauwell.core.settings.ThemeMode
import com.laul.lauwell.core.ui.theme.LauWellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LauWellApplication).container
        val authRepository = container.authRepository
        setContent {
            // Settings › Appearance; System until the stored choice is read.
            val themeMode by container.displayPreferences.themeMode.collectAsState(initial = ThemeMode.System)
            val dark = when (themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }
            // Status and navigation bar icons follow the app's theme, not the phone's.
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            LauWellTheme(darkTheme = dark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LauWellTheme.colors.bg),
                ) {
                    AuthGate(authRepository) {
                        LauWellNavHost()
                    }
                }
            }
        }
    }
}
