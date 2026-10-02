package com.laul.lauwell.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.laul.lauwell.core.common.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * [AuthRepository] backed by Credential Manager (Google account sheet → Google ID token) and
 * Firebase Auth (ID token → Firebase session, stored and refreshed by the SDK).
 *
 * Never logs: the ID token, nonce and account details all stay inside this class.
 *
 * @param webClientId the OAuth "Web application" client id (`BuildConfig.GOOGLE_WEB_CLIENT_ID`);
 *   Google issues ID tokens for this audience, which is what Firebase expects.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val webClientId: String,
) : AuthRepository {

    private val _session = MutableStateFlow(firebaseAuth.currentUser?.toSession())
    override val session: StateFlow<Session?> = _session.asStateFlow()

    init {
        // Keeps [session] in sync with every change: sign-in, sign-out, and Firebase revoking the
        // session (account deleted or disabled). This repository lives as long as the process, so
        // the listener is never removed.
        firebaseAuth.addAuthStateListener { auth -> _session.value = auth.currentUser?.toSession() }
    }

    override suspend fun signIn(activityContext: Context): AppResult<Session> {
        if (webClientId.isBlank()) {
            return AppResult.Error(
                AuthException.Unknown(),
                "GOOGLE_WEB_CLIENT_ID is missing from local.properties",
            )
        }
        return try {
            val nonce = Nonce.generate()
            val idToken = requestGoogleIdToken(activityContext, nonce)
            val credential = OAuthProvider.newCredentialBuilder(GOOGLE_PROVIDER_ID)
                .setIdTokenWithRawNonce(idToken, nonce.raw)
                .build()
            val user = firebaseAuth.signInWithCredential(credential).await().user
                ?: throw AuthException.Unknown()
            AppResult.Success(user.toSession())
        } catch (e: CancellationException) {
            throw e // The calling coroutine was cancelled: not a sign-in failure.
        } catch (e: Exception) {
            AppResult.Error(e.toAuthException())
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            // Forget the account picked last time, so the next sign-in shows the sheet again.
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: ClearCredentialException) {
            // Already signed out of Firebase, which is what matters; nothing else to do.
        }
    }

    /** Shows the Google account sheet and returns the chosen account's ID token. */
    private suspend fun requestGoogleIdToken(activityContext: Context, nonce: Nonce): String {
        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(nonce.hashed)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val credential = credentialManager.getCredential(activityContext, request).credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw AuthException.Unknown()
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private fun FirebaseUser.toSession() = Session(uid = uid)

    private companion object {
        const val GOOGLE_PROVIDER_ID = "google.com"
    }
}
