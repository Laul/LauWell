package com.laul.lauwell.core.auth.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.BuildConfig
import com.laul.lauwell.R
import com.laul.lauwell.core.auth.AuthException
import com.laul.lauwell.core.di.appViewModel
import com.laul.lauwell.core.ui.components.FilledButton
import com.laul.lauwell.core.ui.theme.LauWellTheme

/** Shown by [AuthGate] whenever there is no session. */
@Composable
fun SignInScreen() {
    val viewModel = appViewModel { SignInViewModel(authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SignInContent(state = state, onSignIn = { viewModel.signIn(context) })
}

/**
 * As designed: the six category bars and the wordmark, centred in the free space; the
 * "Continue with Google" button and the privacy line at the bottom, within thumb reach.
 */
@Composable
private fun SignInContent(state: SignInUiState, onSignIn: () -> Unit) {
    val colors = LauWellTheme.colors
    val type = LauWellTheme.typography
    val spacing = LauWellTheme.spacing
    val loading = state == SignInUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(start = spacing.xl + spacing.xxs, end = spacing.xl + spacing.xxs, top = spacing.xl, bottom = spacing.xl + spacing.xxs),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.lg, Alignment.CenterVertically),
        ) {
            CategoryBars()
            BasicText(
                text = "LauWell",
                style = type.display.copy(color = colors.ink),
                modifier = Modifier.semantics { heading() },
            )
            BasicText(
                text = "Vitals, labs, records and care in one place. Kept in your own account.",
                style = type.body.copy(color = colors.ink2, fontSize = 18.sp, lineHeight = 25.sp),
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.panel)) {
            if (state is SignInUiState.Failed) {
                BasicText(
                    text = state.error.userMessage(),
                    style = type.body.copy(color = colors.ink, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FilledButton(text = "", onClick = onSignIn, enabled = !loading) {
                GoogleButtonLabel(if (loading) "Signing in…" else "Continue with Google")
            }
            BasicText(
                text = "Signing in creates a private space only you can read. Nothing is shared.",
                style = type.caption.copy(color = colors.ink2, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The Google "G" (official mark, rendered from Google's Sign in with Google branding assets) on
 * an `onAccent` disc, then the label. The disc keeps the multicolour mark readable on the ink
 * button in both themes.
 */
@Composable
private fun GoogleButtonLabel(text: String) {
    val colors = LauWellTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LauWellTheme.spacing.sm)) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(colors.onAccent),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(R.drawable.ic_google_g), contentDescription = null, modifier = Modifier.size(16.dp))
        }
        BasicText(
            text = text,
            style = LauWellTheme.typography.body.copy(color = colors.onAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * One bar per data category, in the design's order and heights: the LauWell mark. Decorative,
 * so hidden from accessibility. A candidate for the app icon (PLAN.md → backlog).
 */
@Composable
private fun CategoryBars() {
    val colors = LauWellTheme.colors
    val bars = listOf(
        colors.vitals to 28.dp,
        colors.labs to 44.dp,
        colors.medicalRecords to 20.dp,
        colors.lifestyle to 36.dp,
        colors.symptoms to 24.dp,
        colors.treatmentsAndAppliances to 40.dp,
    )
    Row(
        modifier = Modifier.height(44.dp).clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { (color, height) ->
            Box(Modifier.width(14.dp).height(height).clip(BarShape).background(color.fg))
        }
    }
}

private val BarShape = RoundedCornerShape(5.dp)

@Preview(name = "Light", widthDp = 360, heightDp = 760)
@Preview(name = "Dark", widthDp = 360, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignInPreview() {
    LauWellTheme {
        Box(Modifier.fillMaxSize().background(LauWellTheme.colors.bg)) {
            SignInContent(state = SignInUiState.Idle, onSignIn = {})
        }
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
