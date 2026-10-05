package com.laul.lauwell.core.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laul.lauwell.core.auth.AuthRepository

/**
 * The app's front door: [SignInScreen] while there is no session, [signedIn] once there is.
 *
 * Reacts to every session change, so signing out from anywhere (or Firebase revoking the
 * session) lands back on sign-in, and the signed-in content — with its navigation back stack —
 * is discarded rather than left reachable.
 */
@Composable
fun AuthGate(
    authRepository: AuthRepository,
    signedIn: @Composable () -> Unit,
) {
    val session by authRepository.session.collectAsStateWithLifecycle()
    if (session == null) SignInScreen() else signedIn()
}
