package com.laul.lauwell.core.auth

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * A one-time value that binds a Google ID token to a single sign-in attempt.
 *
 * Google receives [hashed] and embeds it in the ID token it issues; Firebase receives [raw],
 * hashes it, and rejects the token if the two don't match. A token intercepted from an earlier
 * attempt therefore can't be replayed.
 */
internal class Nonce private constructor(val raw: String) {

    val hashed: String = sha256Hex(raw)

    companion object {
        fun generate(random: SecureRandom = SecureRandom()): Nonce {
            val bytes = ByteArray(32).also(random::nextBytes)
            return Nonce(bytes.toHex())
        }

        internal fun sha256Hex(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).toHex()

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    }
}
