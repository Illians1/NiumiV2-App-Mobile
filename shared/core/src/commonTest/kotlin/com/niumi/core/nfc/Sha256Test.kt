package com.niumi.core.nfc

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Vecteurs officiels FIPS 180-4 (Secure Hash Standard, §Appendix B).
 */
class Sha256Test {
    @Test
    fun emptyMessage() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.hexOf(Sha256.hash(ByteArray(0))),
        )
    }

    @Test
    fun abcMessage() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.hexOf(Sha256.hash("abc".encodeToByteArray())),
        )
    }

    @Test
    fun oneMillionRepeatedA() {
        val message = ByteArray(1_000_000) { 'a'.code.toByte() }
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            Sha256.hexOf(Sha256.hash(message)),
        )
    }

    @Test
    fun hexOfIsLowercase() {
        val hex = Sha256.hexOf(Sha256.hash("abc".encodeToByteArray()))
        assertEquals(hex, hex.lowercase())
    }
}
