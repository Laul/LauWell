package com.laul.lauwell.core.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMappingTest {

    @Test
    fun `dismissing the account sheet maps to Cancelled`() {
        val error = GetCredentialCancellationException("dismissed")
        val mapped = error.toAuthException()
        assertTrue(mapped is AuthException.Cancelled)
        assertSame(error, mapped.cause)
    }

    @Test
    fun `no Google account on the device maps to NoGoogleAccount`() {
        assertTrue(NoCredentialException("none").toAuthException() is AuthException.NoGoogleAccount)
    }

    // FirebaseNetworkException → Network isn't covered here: constructing any FirebaseException
    // calls android.text.TextUtils, which plain JVM unit tests don't provide.

    @Test
    fun `anything else maps to Unknown`() {
        assertTrue(GetCredentialUnknownException("?").toAuthException() is AuthException.Unknown)
        assertTrue(IllegalStateException().toAuthException() is AuthException.Unknown)
    }

    @Test
    fun `an AuthException passes through unchanged`() {
        val error = AuthException.Network()
        assertSame(error, error.toAuthException())
    }
}
