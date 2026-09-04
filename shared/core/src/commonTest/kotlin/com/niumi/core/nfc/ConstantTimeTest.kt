package com.niumi.core.nfc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConstantTimeTest {
    @Test
    fun equalArraysAreEqual() {
        val a = byteArrayOf(1, 2, 3, 4)
        val b = byteArrayOf(1, 2, 3, 4)
        assertTrue(ConstantTime.equals(a, b))
    }

    @Test
    fun differentArraysAreNotEqual() {
        val a = byteArrayOf(1, 2, 3, 4)
        val b = byteArrayOf(1, 2, 3, 5)
        assertFalse(ConstantTime.equals(a, b))
    }

    @Test
    fun differentLengthsAreNotEqual() {
        val a = byteArrayOf(1, 2, 3)
        val b = byteArrayOf(1, 2, 3, 4)
        assertFalse(ConstantTime.equals(a, b))
    }

    @Test
    fun emptyArraysAreEqual() {
        assertTrue(ConstantTime.equals(ByteArray(0), ByteArray(0)))
    }
}
