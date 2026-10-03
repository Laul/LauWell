package com.laul.lauwell.core.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException

/**
 * Turns whatever Credential Manager or Firebase threw during sign-in into an [AuthException].
 *
 * Kept separate from [FirebaseAuthRepository] so it can be unit-tested on the JVM.
 */
internal fun Throwable.toAuthException(): AuthException = when (this) {
    is AuthException -> this
    is GetCredentialCancellationException -> AuthException.Cancelled(this)
    is NoCredentialException -> AuthException.NoGoogleAccount(this)
    is FirebaseNetworkException -> AuthException.Network(this)
    else -> AuthException.Unknown(this)
}
