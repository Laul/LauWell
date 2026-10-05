package com.laul.lauwell.core.auth

/**
 * Why a sign-in failed, carried as the throwable of an [com.laul.lauwell.core.common.AppResult.Error].
 *
 * Typed so the sign-in screen can react to each case (stay silent on [Cancelled], suggest adding
 * an account on [NoGoogleAccount], offer a retry on [Network]) instead of showing raw messages.
 */
sealed class AuthException(cause: Throwable? = null) : Exception(cause) {

    /** The user dismissed the Google account sheet. Not an error to show. */
    class Cancelled(cause: Throwable? = null) : AuthException(cause)

    /** No Google account on the device, or none usable for this app. */
    class NoGoogleAccount(cause: Throwable? = null) : AuthException(cause)

    /** Couldn't reach Google or Firebase. */
    class Network(cause: Throwable? = null) : AuthException(cause)

    /** Anything else (misconfiguration, unexpected credential type, Firebase rejection…). */
    class Unknown(cause: Throwable? = null) : AuthException(cause)
}
