package com.laul.lauwell.core.auth

import android.content.Context
import com.laul.lauwell.core.common.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Sign-in state and actions for the whole app. The only way UI code talks to authentication.
 *
 * Implementation: [FirebaseAuthRepository] (Credential Manager → Google ID token → Firebase Auth),
 * built once in [com.laul.lauwell.core.di.AppContainer].
 */
interface AuthRepository {

    /**
     * The current session, or `null` when signed out.
     *
     * A [StateFlow] so it always has a value: Firebase restores a previous session synchronously
     * at startup, so the app can choose between the sign-in screen and Home on the first frame,
     * without a loading flash.
     */
    val session: StateFlow<Session?>

    /**
     * Shows the Google account sheet and signs in with the chosen account.
     *
     * @param activityContext must be an Activity: Credential Manager anchors its sheet to it.
     * @return [AppResult.Success] with the new session, or [AppResult.Error] whose throwable is
     *   an [AuthException].
     */
    suspend fun signIn(activityContext: Context): AppResult<Session>

    /** Signs out of Firebase and clears the remembered Google account choice. */
    suspend fun signOut()
}
