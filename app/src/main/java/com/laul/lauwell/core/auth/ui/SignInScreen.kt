package com.laul.lauwell.core.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.BuildConfig
import com.laul.lauwell.core.auth.AuthException
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** Shown by [AuthGate] whenever there is no session. */
@Composable
fun SignInScreen() {
    val viewModel = appViewModel { SignInViewModel(authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SignInContent(state = state, onSignIn = { viewModel.signIn(context) })
}

@Composable
private fun SignInContent(state: SignInUiState, onSignIn: () -> Unit) {
    val loading = state == SignInUiState.Loading
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = "LauWell",
            style = LauWellTheme.typography.title.copy(color = LauWellTheme.colors.onBackground),
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = "Your health data, in one place.",
            style = LauWellTheme.typography.body.copy(color = LauWellTheme.colors.onBackground),
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton(
            text = if (loading) "Signing in…" else "Sign in with Google",
            enabled = !loading,
            onClick = onSignIn,
        )
        if (state is SignInUiState.Failed) {
            Spacer(Modifier.height(16.dp))
            BasicText(
                text = state.error.userMessage(),
                style = LauWellTheme.typography.body.copy(
                    color = LauWellTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

/** Full-width button from our own tokens (no Material dependency). 56dp tall: an easy target. */
@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(LauWellTheme.colors.primary)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = LauWellTheme.typography.label.copy(color = LauWellTheme.colors.onPrimary),
        )
    }
}

private fun AuthException.userMessage(): String {
    val message = when (this) {
        is AuthException.NoGoogleAccount ->
            "No Google account found on this device. Add one in the phone's Settings, then try again."
        is AuthException.Network -> "Couldn't connect. Check your internet connection and try again."
        is AuthException.Cancelled, is AuthException.Unknown -> "Sign-in didn't work. Please try again."
    }
    // Debug builds only: name the underlying error type to help diagnose setup problems.
    // Only the class name: exception messages can contain account details.
    val cause = cause?.javaClass?.simpleName
    return if (BuildConfig.DEBUG && cause != null) "$message\n($cause)" else message
}
