package com.laul.lauwell.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NonceTest {

    @Test
    fun `hashed is the SHA-256 hex of raw`() {
        val nonce = Nonce.generate()
        assertEquals(Nonce.sha256Hex(nonce.raw), nonce.hashed)
    }

    @Test
    fun `sha256Hex matches a known vector`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Nonce.sha256Hex("abc"),
        )
    }

    @Test
    fun `raw is 32 random bytes as hex`() {
        val raw = Nonce.generate().raw
        assertEquals(64, raw.length)
        assert(raw.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `each sign-in attempt gets a fresh nonce`() {
        assertNotEquals(Nonce.generate().raw, Nonce.generate().raw)
    }
}
